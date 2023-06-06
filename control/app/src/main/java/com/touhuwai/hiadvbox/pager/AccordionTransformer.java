/*
 * Copyright 2014 Toxic Bakery
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.touhuwai.hiadvbox.pager;

import android.view.View;

public class AccordionTransformer extends ABaseTransformer {
	private static final float MIN_ALPHA = 0.0f;    //最小透明度
	@Override
	protected void onTransform(View view, float position) {
		int pageWidth = view.getWidth();    //得到view宽
		if (position < -1) { // [-Infinity,-1)
			// This page is way off-screen to the left. 出了左边屏幕
			view.setAlpha(0);
		} else if (position <= 1) { // [-1,1]
			if (position < 0) {
				//消失的页面
				view.setTranslationX(-pageWidth * position);  //阻止消失页面的滑动
			} else {
				//出现的页面
				view.setTranslationX(pageWidth);        //直接设置出现的页面到底
				view.setTranslationX(-pageWidth * position);  //阻止出现页面的滑动
			}
			// Fade the page relative to its size.
			float alphaFactor = Math.max(MIN_ALPHA, 1 - Math.abs(position));
			//透明度改变Log
			view.setAlpha(alphaFactor);
			// 控制展示层级
			view.setTranslationZ(-position);
		} else { // (1,+Infinity]
			// This page is way off-screen to the right.    出了右边屏幕
			view.setAlpha(0);
		}
	}

}
