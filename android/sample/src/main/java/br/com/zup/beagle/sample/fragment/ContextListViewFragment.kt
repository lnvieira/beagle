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

package br.com.zup.beagle.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.zup.beagle.android.action.RequestActionMethod
import br.com.zup.beagle.android.action.SendRequest
import br.com.zup.beagle.android.action.SetContext
import br.com.zup.beagle.android.components.Button
import br.com.zup.beagle.android.components.ListView
import br.com.zup.beagle.android.components.Text
import br.com.zup.beagle.android.components.layout.Container
import br.com.zup.beagle.android.components.layout.NavigationBar
import br.com.zup.beagle.android.components.layout.Screen
import br.com.zup.beagle.android.context.ContextData
import br.com.zup.beagle.android.context.expressionOf
import br.com.zup.beagle.android.utils.toView
import br.com.zup.beagle.core.Style
import br.com.zup.beagle.ext.applyStyle
import br.com.zup.beagle.ext.unitPercent
import br.com.zup.beagle.ext.unitReal
import br.com.zup.beagle.widget.core.ListDirection
import br.com.zup.beagle.widget.core.Size

class ContextListViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val declarative = Screen(
            navigationBar = NavigationBar(title = "List"),
            child = list
        )
        return context?.let { declarative.toView(this) }
    }

    private val list = ListView(
        context = ContextData(
            id = "insideContext",
            value = listOf("1 inside", "2 inside", "3 inside", "4 inside", "5 inside",
                "6 inside", "7 inside", "8 inside", "9 inside", "10 inside",
                "11 inside", "12 inside", "13 inside", "14 inside", "15 inside",
                "16 inside", "17 inside", "18 inside", "19 inside", "20 inside"
            )
        ),
        dataSource = expressionOf("@{insideContext}"),
        direction = ListDirection.HORIZONTAL,
        template = Container(
            children = listOf(
                Button(
                    text = expressionOf("@{item}"),
                    onPress = listOf(
                        SetContext(
                            contextId = "insideContext",
                            path = "[0]",
                            value = "0 inside"
                        )
                    )
                ).applyStyle(
                    Style(
                        size = Size(width = 300.unitReal(), height = 80.unitReal())
                    )
                )
            )
        )
    )

    private fun buildListView() = ListView(
        context = ContextData(
            id = "outsideContext",
            value = listOf("1 OUTSIDE"/*, "2 OUTSIDE", "3 OUTSIDE", "4 OUTSIDE", "5 OUTSIDE",
                "6 OUTSIDE", "7 OUTSIDE", "8 OUTSIDE", "9 OUTSIDE", "10 OUTSIDE"*/)
        ),
        dataSource = expressionOf("@{outsideContext}"),
        direction = ListDirection.VERTICAL,
        template = Container(
            children = listOf(
                Text(text = expressionOf("@{item}")),
                list
            )
        ).applyStyle(
            Style(
                size = Size(width = 100.unitPercent(), height = 300.unitReal())
            )
        )
    )

    data class Movie(
        val poster_path: String?,
        val original_title: String,
        val backdrop_path: String?
    )

    private val listMovies = ListView(
        context = ContextData(
            id = "movieContext",
            value = listOf(
                Movie(
                    poster_path = "",
                    original_title = "",
                    backdrop_path = ""
                )
            )
        ),
        onInit = listOf(
            SendRequest(
                url = "https://api.themoviedb.org/3/discover/movie?api_key=d272326e467344029e68e3c4ff0b4059&with_genres=28",
                method = RequestActionMethod.GET,
                onSuccess = listOf(
                    SetContext(
                        contextId = "movieContext",
                        value = "@{onSuccess.data.results}"
                    )
                )
            )
        ),
        dataSource = expressionOf("@{movieContext}"),
        direction = ListDirection.HORIZONTAL,
        template = Text(text = expressionOf("@{item.original_title}"))
    )

    private val listGenres = ListView(
        context = ContextData(
            id = "initialContext",
            value = listOf(
                "1fora",
                "2fora",
                "3fora",
                "4fora",
                "5fora",
                "6fora",
                "7fora",
                "8fora",
                "9fora",
                "10fora",
                "11fora",
                "12fora",
                "13fora",
                "14fora",
                "15fora",
                "16fora",
                "17fora",
                "18fora",
                "19fora",
                "20fora"
            )
        ),
        dataSource = expressionOf("@{initialContext}"),
        direction = ListDirection.VERTICAL,
        template = Container(
            listOf(
                Text(text = expressionOf("@{item}")),
                listMovies.applyStyle(Style(backgroundColor = "#b7472a"))
            )
        ).applyStyle(
            Style(
                backgroundColor = "#2a7886",
                size = Size(width = 100.unitPercent(), height = 300.unitReal())
            )
        )
    )

    companion object {

        fun newInstance(): ContextListViewFragment {
            return ContextListViewFragment()
        }
    }
}