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

import android.view.View
import br.com.zup.beagle.android.utils.ComponentConstant.DEPRECATED_TAB_VIEW
import br.com.zup.beagle.android.view.ComponentsViewFactory
import br.com.zup.beagle.android.widget.RootView
import br.com.zup.beagle.android.widget.WidgetView
import br.com.zup.beagle.annotation.RegisterWidget
import br.com.zup.beagle.core.ServerDrivenComponent

@RegisterWidget
@Deprecated(DEPRECATED_TAB_VIEW)
data class TabItem(
    val title: String? = null,
    val child: ServerDrivenComponent,
    val icon: PathType.Local? = null
) : WidgetView() {

    @Transient
    private val componentsViewFactory = ComponentsViewFactory()

    override fun buildView(rootView: RootView): View {
        return componentsViewFactory.makeBeagleFlexView(rootView.getContext()).also {
            it.addServerDrivenComponent(child, rootView)
        }
    }
}