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

package com.mobsandgeeks.saripaar.annotation;

import android.support.annotation.StringRes;

import com.mobsandgeeks.saripaar.rule.CreditCardRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
@ValidateUsing(CreditCardRule.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CreditCard {
    CreditCard.Type[] cardTypes() default {CreditCard.Type.AMEX, CreditCard.Type.DINERS, CreditCard.Type.DISCOVER, CreditCard.Type.MASTERCARD, CreditCard.Type.VISA, CreditCard.Type.MAESTRO, CreditCard.Type.JCB, CreditCard.Type.UNIONPAY, CreditCard.Type.MIR, CreditCard.Type.INTERPAYMENT, CreditCard.Type.UATP};

    int sequence() default -1;

    int messageResId() default -1;

    String message() default "Invalid card";

    int flags() default 0;

    int errorCode() default -1;

    public static enum Type {
        AMEX,
        DINERS,
        DISCOVER,
        MASTERCARD,
        VISA,
        MAESTRO,
        JCB,
        UNIONPAY,
        MIR,
        INTERPAYMENT,
        UATP,
        NONE;

        private Type() {
        }
    }
}
