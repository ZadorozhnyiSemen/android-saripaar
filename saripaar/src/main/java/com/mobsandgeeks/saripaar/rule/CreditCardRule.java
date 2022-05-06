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

package com.mobsandgeeks.saripaar.rule;

import com.mobsandgeeks.saripaar.AnnotationRule;
import com.mobsandgeeks.saripaar.annotation.CreditCard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import commons.validator.routines.CreditCardValidator;

/**
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
public class CreditCardRule extends AnnotationRule<CreditCard, String> {
    private static final Map<CreditCard.Type, Long> CARD_TYPE_REGISTRY = new HashMap<CreditCard.Type, Long>() {
        {
            this.put(CreditCard.Type.AMEX, CreditCardValidator.AMEX);
            this.put(CreditCard.Type.DINERS, CreditCardValidator.DINERS);
            this.put(CreditCard.Type.DISCOVER, CreditCardValidator.DISCOVER);
            this.put(CreditCard.Type.MASTERCARD, CreditCardValidator.MASTERCARD);
            this.put(CreditCard.Type.VISA, CreditCardValidator.VISA);
            this.put(CreditCard.Type.MAESTRO, CreditCardValidator.MAESTRO);
            this.put(CreditCard.Type.JCB, CreditCardValidator.JCB);
            this.put(CreditCard.Type.UNIONPAY, CreditCardValidator.UNIONPAY);
            this.put(CreditCard.Type.MIR, CreditCardValidator.MIR);
            this.put(CreditCard.Type.INTERPAYMENT, CreditCardValidator.INTERPAYMENT);
            this.put(CreditCard.Type.UATP, CreditCardValidator.UATP);
        }
    };

    protected CreditCardRule(CreditCard creditCard) {
        super(creditCard);
    }

    public boolean isValid(String creditCardNumber) {
        CreditCard.Type[] types = ((CreditCard)this.mRuleAnnotation).cardTypes();
        HashSet<CreditCard.Type> typesSet = new HashSet(Arrays.asList(types));
        long options = 0L;
        CreditCard.Type type;
        if (!typesSet.contains(CreditCard.Type.NONE)) {
            for(Iterator var6 = typesSet.iterator(); var6.hasNext(); options += (Long)CARD_TYPE_REGISTRY.get(type)) {
                type = (CreditCard.Type)var6.next();
            }
        } else {
            options = 0L;
        }

        return (new CreditCardValidator(options)).isValid(creditCardNumber.replaceAll("\\s", ""));
    }
}
