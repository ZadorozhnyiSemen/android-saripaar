/*
 * Copyright (C) 2014 Mobs & Geeks
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobsandgeeks.saripaar.adapter;

import android.view.View;

import com.mobsandgeeks.saripaar.exception.ConversionException;

import java.lang.annotation.Annotation;

/**
 * {@link com.mobsandgeeks.saripaar.adapter.ViewDataAdapter}s are used to extract data from
 * {@link android.view.View}s. Saripaar provides a set of default adapters for stock Android
 * widgets. Developers can implement their own adapters for custom views or data types they are
 * interested in.
 *
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
public interface ViewDataAdapter<VIEW extends View, DATA> {
    DATA getData(VIEW var1) throws ConversionException;
}