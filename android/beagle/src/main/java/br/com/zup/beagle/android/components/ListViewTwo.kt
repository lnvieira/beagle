/*
 * Copyright 2020 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.zup.beagle.android.components

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.zup.beagle.android.action.Action
import br.com.zup.beagle.android.context.Bind
import br.com.zup.beagle.android.context.ContextComponent
import br.com.zup.beagle.android.context.ContextData
import br.com.zup.beagle.android.data.serializer.BeagleMoshi
import br.com.zup.beagle.android.data.serializer.BeagleSerializer
import br.com.zup.beagle.android.utils.getContextData
import br.com.zup.beagle.android.utils.observeBindChanges
import br.com.zup.beagle.android.utils.setContextData
import br.com.zup.beagle.android.utils.toView
import br.com.zup.beagle.android.view.ViewFactory
import br.com.zup.beagle.android.widget.RootView
import br.com.zup.beagle.android.widget.WidgetView
import br.com.zup.beagle.annotation.RegisterWidget
import br.com.zup.beagle.core.ServerDrivenComponent
import br.com.zup.beagle.widget.core.FlexDirection.COLUMN
import br.com.zup.beagle.widget.core.FlexDirection.ROW
import br.com.zup.beagle.widget.core.ListDirection

@RegisterWidget
internal data class ListViewTwo(
    override val context: ContextData? = null,
    val onInit: List<Action>? = null,
    val dataSource: Bind<List<Any>>,
    val direction: ListDirection,
    val template: ServerDrivenComponent,
    val onScrollEnd: List<Action>? = null,
    val scrollThreshold: Int? = null,
    val useParentScroll: Boolean = false
) : WidgetView(), ContextComponent {

    @Transient
    private val viewFactory: ViewFactory = ViewFactory()

    @Transient
    private lateinit var contextAdapter: ListViewContextAdapter2

    @Transient
    private var list: List<Any>? = null

    override fun buildView(rootView: RootView): View {
        val recyclerView = viewFactory.makeRecyclerView(rootView.getContext())
        //onInit?.forEach { action ->
         //   action.execute(rootView, recyclerView)
        //}
        val orientation = toRecyclerViewOrientation()
        contextAdapter = ListViewContextAdapter2(template, viewFactory, orientation, rootView, onInit)
        recyclerView.apply {
            adapter = contextAdapter
            layoutManager = LinearLayoutManager(context, orientation, false)
            isNestedScrollingEnabled = useParentScroll
        }
        configDataSourceObserver(rootView, recyclerView)
        configRecyclerViewScrollListener(recyclerView, rootView)

        var onInitCalled = false
        recyclerView.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewDetachedFromWindow(v: View?) {
            }

            override fun onViewAttachedToWindow(v: View?) {
                if(!onInitCalled){
                    onInit?.forEach { action ->
                        action.execute(rootView, recyclerView)
                    }
                    onInitCalled = true
                }
            }

        })

        return recyclerView
    }

    private fun toRecyclerViewOrientation() = if (direction == ListDirection.VERTICAL) {
        RecyclerView.VERTICAL
    } else {
        RecyclerView.HORIZONTAL
    }

    private fun configDataSourceObserver(rootView: RootView, recyclerView: RecyclerView) {
        observeBindChanges(rootView, recyclerView, dataSource) { value ->
            if (value != list) {
                if (value.isNullOrEmpty()) {
                    (recyclerView.adapter as ListViewContextAdapter2).clearList()
                } else {
                    (recyclerView.adapter as ListViewContextAdapter2).setList(value)
                }
                list = value
            }

        }
    }

    private fun configRecyclerViewScrollListener(
        recyclerView: RecyclerView,
        rootView: RootView
    ) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (needToExecuteOnScrollEnd(recyclerView)) {
                    onScrollEnd?.forEach { action ->
                        action.execute(rootView, recyclerView)
                    }
                }
            }
        })
    }

    private fun needToExecuteOnScrollEnd(recyclerView: RecyclerView): Boolean {
        val scrolledPercent = calculateScrolledPercent(recyclerView)
        scrollThreshold?.let {
            return scrolledPercent >= scrollThreshold
        }
        return scrolledPercent == 100f
    }

    private fun calculateScrolledPercent(recyclerView: RecyclerView): Float {
        val layoutManager = LinearLayoutManager::class.java.cast(recyclerView.layoutManager)
        var scrolled = 0f
        layoutManager?.let {
            val totalItemCount = it.itemCount.toFloat()
            val lastVisible = it.findLastVisibleItemPosition().toFloat()
            scrolled = (lastVisible / totalItemCount) * 100
        }

        return scrolled
    }
}

internal class ListViewContextAdapter2(
    private val template: ServerDrivenComponent,
    private val viewFactory: ViewFactory,
    private val orientation: Int,
    private val rootView: RootView,
    val onInit: List<Action>?,
    private var listItems: ArrayList<Any> = ArrayList()
) : RecyclerView.Adapter<ContextViewHolderTwo>() {

    //TODO: vou ter que guardar todos os contextos de todos os filhos???
    val viewPool = RecyclerView.RecycledViewPool()
    class BeagleAdapterItem(val data: Any,
                            var childContextList: MutableList<String?> = mutableListOf(),
                            var initialized: Boolean = false)

    var adapterItens = listOf<BeagleAdapterItem>()

    private val templateJson = BeagleSerializer().serializeComponent(template)

    var viewHolder = 0

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContextViewHolderTwo {

        val newTemplate = BeagleSerializer().deserializeComponent(templateJson)

        val view = newTemplate
            .toView(rootView)
            .also {
                it.layoutParams = LayoutParams(layoutParamWidth(), layoutParamHeight())
            }

        val recyclerViewList = mutableListOf<RecyclerView>()
        findNestedRecyclerViews(view, recyclerViewList)
        recyclerViewList.forEach {
            it.setRecycledViewPool(viewPool)
            //adapterItens[viewHolder].childAdapterList.add(it.adapter as ListViewContextAdapter2)
            //adapterItens[viewHolder].childContextList.add(it.getContextData())
            adapterItens[viewHolder].initialized = true
        }
        //TODO: esse index está causando problemas
        viewHolder++
        val holder = ContextViewHolderTwo(view)
        return holder
    }

    private fun layoutParamWidth() = if (isOrientationVertical()) MATCH_PARENT else WRAP_CONTENT

    private fun layoutParamHeight() = if (isOrientationVertical()) WRAP_CONTENT else MATCH_PARENT

    private fun flexDirection() = if (isOrientationVertical()) COLUMN else ROW

    private fun isOrientationVertical() = (orientation == RecyclerView.VERTICAL)

    override fun onBindViewHolder(holder: ContextViewHolderTwo, position: Int) {
        holder.itemView.setContextData(ContextData(id = "item", value = adapterItens[position].data))

        val recyclerViewList = mutableListOf<RecyclerView>()
        findNestedRecyclerViews(holder.itemView, recyclerViewList)
        var recyclerIndex = 0
        recyclerViewList.forEach{
            if(!adapterItens[position].initialized) {
                val recyclerAdapter = it.adapter as ListViewContextAdapter2
                recyclerAdapter.executeInit(it)
            }else {
                adapterItens[position].childContextList.getOrNull(recyclerIndex)?.let{sContext ->
                    val context = BeagleMoshi.moshi.adapter(ContextData::class.java).fromJson(sContext)
                    context?.let {context ->
                        it.setContextData(context)
                    }
                }
            }

            recyclerIndex++
        }
        adapterItens[position].initialized = true
    }

    override fun onViewDetachedFromWindow(holder: ContextViewHolderTwo) {
        super.onViewDetachedFromWindow(holder)
        val recyclerViewList = mutableListOf<RecyclerView>()
        findNestedRecyclerViews(holder.itemView, recyclerViewList)
        var recyclerIndex = 0
        recyclerViewList.forEach{
            val stringContext = BeagleMoshi.moshi.adapter(ContextData::class.java).toJson(it.getContextData())
            adapterItens[ holder.adapterPosition].childContextList.add(recyclerIndex, stringContext)
            recyclerIndex++
        }
    }

    fun executeInit(recyclerView: RecyclerView){
        onInit?.forEach { action ->
            action.execute(rootView, recyclerView)
        }
    }

    fun setList(list: List<Any>) {
        try {
            adapterItens = list.map { BeagleAdapterItem(it) }
            notifyDataSetChanged()
        } catch (e: Exception) {

        }
    }

    fun clearList() {
        val initialSize = listItems.size
        listItems.clear()
        notifyItemRangeRemoved(0, initialSize)
    }

    override fun getItemCount(): Int = adapterItens.size

    fun findNestedRecyclerViews(view: View, recyclerList: MutableList<RecyclerView>) {
        if (view !is ViewGroup) {
            return
        }
        if (view is RecyclerView) {
            recyclerList.add(view)
            return
        }
        val parent = view
        val count = parent.childCount
        for (i in 0 until count) {
            val child = parent.getChildAt(i)
            findNestedRecyclerViews(child, recyclerList)
            /*if (descendant != null) {
                return descendant
            }*/
        }
        //return null
    }
}

internal class ContextViewHolderTwo(itemView: View) : RecyclerView.ViewHolder(itemView) {
}
