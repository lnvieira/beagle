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

package br.com.zup.beagle.android.context

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.collection.LruCache
import androidx.core.view.children
import br.com.zup.beagle.android.action.SetContextInternal
import br.com.zup.beagle.android.logger.BeagleMessageLogs
import br.com.zup.beagle.android.utils.Observer
import br.com.zup.beagle.android.utils.findParentContextWithId
import br.com.zup.beagle.android.utils.getAllParentContexts
import br.com.zup.beagle.android.utils.getContextBinding
import br.com.zup.beagle.android.utils.getContextId
import br.com.zup.beagle.android.utils.getExpressions
import br.com.zup.beagle.android.utils.getParentContextBinding
import br.com.zup.beagle.android.utils.setContextBinding
import br.com.zup.beagle.android.utils.setContextData

private const val GLOBAL_CONTEXT_ID = Int.MAX_VALUE

internal class ContextDataManager(
    private val contextDataEvaluation: ContextDataEvaluation = ContextDataEvaluation(),
    private val contextDataManipulator: ContextDataManipulator = ContextDataManipulator()
) {

    private var globalContext: ContextBinding = ContextBinding(GlobalContext.getContext())
    private val contexts = mutableMapOf<Int, ContextBinding>()
    private val bindingQueue = mutableMapOf<View, MutableList<Binding<*>>>() // TODO: verificar se mudar para lista dá problema
    private val globalContextObserver: GlobalContextObserver = {
        updateGlobalContext(it)
    }

    init {
        contexts[GLOBAL_CONTEXT_ID] = globalContext
        GlobalContext.observeGlobalContextChange(globalContextObserver)
    }

    fun clearContexts() {
        contexts.clear()
        bindingQueue.clear()
        GlobalContext.clearObserverGlobalContext(globalContextObserver)
    }

    fun clearContext(view: View) {
        contexts.remove(view.id)
        bindingQueue.remove(view)
    }

    /**
     * TODO: verificar esse método aqui, pela questão do viewID não ser previsível em listas dentro de listas
     */
    fun addContext(view: View, context: ContextData) {

        if (context.id == globalContext.context.id) {
            BeagleMessageLogs.globalKeywordIsReservedForGlobalContext()
            return
        }

        val existingContext = contexts[view.id]

        if (existingContext != null) {
            view.setContextBinding(existingContext.copy(context = context))
            existingContext.bindings.clear()
        } else {
            view.setContextData(context)
            view.getContextBinding()?.let {
                contexts[view.id] = it
            }
        }
    }

    fun <T> addBinding(view: View, bind: Bind.Expression<T>, observer: Observer<T?>) {
        val bindings: MutableList<Binding<*>> = bindingQueue[view] ?: mutableListOf()
        bindings.add(Binding(
            observer = observer,
            bind = bind
        ))
        bindingQueue[view] = bindings
    }

    /**
     * TODO: verificar se existe alguma hipótese de uma view filha ser "resolvida" antes do pai
     */
    fun resolveBindings(view: View) {
        if(bindingQueue.contains(view)) {
            val contextStack = view.getAllParentContextWithGlobal()

            bindingQueue[view]?.forEach { binding ->
                binding.bind.value.getExpressions().forEach { expression ->
                    val contextId = expression.getContextId()

                    for (contextBinding in contextStack) {
                        if (contextBinding.context.id == contextId) {
                            contextBinding.bindings.add(binding)
                            val value = contextDataEvaluation.evaluateBindExpression(
                                contextBinding.context,
                                contextBinding.cache,
                                binding.bind,
                                binding.evaluatedExpressions
                            )
                            binding.notifyChanges(value)
                            break
                        }
                    }
                }
            }
            bindingQueue.remove(view)
        }

        view.getContextBinding()?.let {
            notifyBindingChanges(it)
        }
    }

    /**
     * TODO: testar contextos implícitos
     */
    fun getContextsFromBind(originView: View, binding: Bind.Expression<*>): List<ContextData> {
        val parentContexts = originView.getAllParentContextWithGlobal()
        val contextIds = binding.value.getExpressions().map { it.getContextId() }
        //return parentContexts.filterKeys { contextIds.contains(it) }.map { it.value.context }
        return parentContexts
            .filter { contextBinding -> contextIds.contains(contextBinding.context.id)}
            .map { it.context }
    }

    fun updateContext(view: View, setContextInternal: SetContextInternal) {
        var contextStack = ""
        view.getAllParentContexts().forEach { contextBinding ->
            contextStack += " - ${contextBinding.context.id}"
        }
        Log.wtf("context", "$contextStack")
        if (setContextInternal.contextId == globalContext.context.id) {
            GlobalContext.set(setContextInternal.value, setContextInternal.path)
        } else {
            view.findParentContextWithId(setContextInternal.contextId)?.let { parentView ->
                val currentContextBinding = parentView.getContextBinding()
                currentContextBinding?.let {
                    setContextValue(parentView, currentContextBinding, setContextInternal)
                }
            }
        }
    }

    private fun setContextValue(
        contextView: View,
        contextBinding: ContextBinding,
        setContextInternal: SetContextInternal
    ) {
        val result = contextDataManipulator.set(
            contextBinding.context,
            setContextInternal.path,
            setContextInternal.value
        )
        if (result is ContextSetResult.Succeed) {
            val newContextBinding = contextBinding.copy(context = result.newContext)
            newContextBinding.cache.evictAll()
            contextView.setContextBinding(newContextBinding)
            contexts[contextView.id] = newContextBinding
            notifyBindingChanges(newContextBinding)
        }
    }

    fun evaluateContexts() {
        contexts.forEach { entry ->
            notifyBindingChanges(entry.value)
        }
    }

    fun notifyBindingChanges(contextBinding: ContextBinding) {
        val contextData = contextBinding.context
        val bindings = contextBinding.bindings

        bindings.forEach { binding ->
            val value = contextDataEvaluation.evaluateBindExpression(
                contextData,
                contextBinding.cache,
                binding.bind,
                binding.evaluatedExpressions
            )
            binding.notifyChanges(value)
        }
    }

    private fun View.getAllParentContextWithGlobal(): MutableList<ContextBinding> {
        val contexts = mutableListOf<ContextBinding>()
        contexts.addAll(this.getAllParentContexts())
        contexts.add(globalContext)
        return contexts
    }

    private fun updateGlobalContext(contextData: ContextData) {
        globalContext = globalContext.copy(context = contextData)
        contexts[GLOBAL_CONTEXT_ID] = globalContext
        notifyBindingChanges(globalContext)
    }
}
