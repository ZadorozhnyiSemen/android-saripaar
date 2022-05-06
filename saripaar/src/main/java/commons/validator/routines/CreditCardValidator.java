/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package commons.validator.routines;

import commons.validator.routines.checkdigit.CheckDigit;
import commons.validator.routines.checkdigit.LuhnCheckDigit;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Perform credit card validations.
 *
 * <p>
 * By default, all supported card types are allowed.  You can specify which
 * cards should pass validation by configuring the validation options. For
 * example,
 * </p>
 *
 * <pre>
 * <code>CreditCardValidator ccv = new CreditCardValidator(CreditCardValidator.AMEX + CreditCardValidator.VISA);</code>
 * </pre>
 *
 * <p>
 * configures the validator to only pass American Express and Visa cards.
 * If a card type is not directly supported by this class, you can implement
 * the CreditCardType interface and pass an instance into the
 * <code>addAllowedCardType</code> method.
 * </p>
 *
 * <p>
 * For a similar implementation in Perl, reference Sean M. Burke's
 * <a href="http://www.speech.cs.cmu.edu/~sburke/pub/luhn_lib.html">script</a>.
 * More information can be found in Michael Gilleland's essay 
 * <a href="http://web.archive.org/web/20120614072656/http://www.merriampark.com/anatomycc.htm">Anatomy of Credit Card Numbers</a>.
 * </p>
 *
 * @version $Revision$
 * @since Validator 1.4
 */
public class CreditCardValidator implements Serializable {

    private static final long serialVersionUID = 5955978921148959496L;

    /**
     * Option specifying that no cards are allowed.  This is useful if
     * you want only custom card types to validate so you turn off the
     * default cards with this option.
     *
     * <pre>
     * <code>
     * CreditCardValidator v = new CreditCardValidator(CreditCardValidator.NONE);
     * v.addAllowedCardType(customType);
     * v.isValid(aCardNumber);
     * </code>
     * </pre>
     */
    public static final long NONE = 0;

    /**
     * Option specifying that American Express cards are allowed.
     */
    public static final long AMEX = 1;

    /**
     * Option specifying that Visa cards are allowed.
     */
    public static final long VISA = 1 << 1;

    /**
     * Option specifying that Mastercard cards are allowed.
     */
    public static final long MASTERCARD = 1 << 2;

    /**
     * Option specifying that Discover cards are allowed.
     */
    public static final long DISCOVER = 1 << 3;

    /**
     * Option specifying that Diners cards are allowed.
     */
    public static final long DINERS = 1 << 4;

    public static final long MAESTRO = 1 << 5;
    public static final long JCB = 1 << 6;
    public static final long UNIONPAY = 1 << 7;
    public static final long MIR = 1 << 8;
    public static final long INTERPAYMENT = 1 << 9;
    public static final long INSTAPAYMENT = 1 << 10;
    public static final long UATP = 1 << 11;

    /**
     * The CreditCardTypes that are allowed to pass validation.
     */
    private final List cardTypes;

    private static final RegexValidator AMEX_REGEX = new RegexValidator(new String[]{"^(3[47]\\d{13})$"});
    private static final RegexValidator VISA_REGEX = new RegexValidator(new String[]{"^(4)(\\d{12}|\\d{15}|\\d{19})$"});
    private static final RegexValidator MASTERCARD_REGEX = new RegexValidator(new String[]{"^(5[1-5]\\d{14})$", "^((222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)\\d{12})$"});
    private static final RegexValidator DISCOVER_REGEX = new RegexValidator(new String[]{"^(6011\\d{12})$", "^(64[4-9]\\d{13})$", "^(65\\d{14})$", "^(62212[6-9]|6221[3-9]|622[2-8]|62290|62291|62292[0-5]\\d{10})$"});
    private static final RegexValidator DINERS_REGEX = new RegexValidator(new String[]{"^(30[0-5]\\d{11}|3095\\d{10}|3[68-9]\\d{12})$"});
    private static final RegexValidator MAESTRO_REGEX = new RegexValidator(new String[]{"^(5[06-9]\\d{10,17})|(6[0-9]\\d{10,17})$"});
    private static final RegexValidator JCB_REGEX = new RegexValidator(new String[]{"^(352[89]\\d{12})|(35[3-8][0-9]\\d{12})$"});
    private static final RegexValidator UNIONPAY_REGEX = new RegexValidator(new String[]{"^(62\\d{14})$"});
    private static final RegexValidator MIR_REGEX = new RegexValidator(new String[]{"^(220[0-4]\\d{12,15})$"});
    private static final RegexValidator INTERPAYMENT_REGEX = new RegexValidator(new String[]{"^(636\\d{13})$"});
    private static final RegexValidator UATP_REGEX = new RegexValidator(new String[]{"^(1\\d{14})$"});

    /**
     * Luhn checkdigit validator for the card numbers.
     */
    private static final CheckDigit LUHN_VALIDATOR = LuhnCheckDigit.LUHN_CHECK_DIGIT;
    public static final CodeValidator AMEX_VALIDATOR;
    public static final CodeValidator VISA_VALIDATOR;
    public static final CodeValidator MASTERCARD_VALIDATOR;
    public static final CodeValidator DISCOVER_VALIDATOR;
    public static final CodeValidator DINERS_VALIDATOR;
    public static final CodeValidator MAESTRO_VALIDATOR;
    public static final CodeValidator JCB_VALIDATOR;
    public static final CodeValidator UNIONPAY_VALIDATOR;
    public static final CodeValidator MIR_VALIDATOR;
    public static final CodeValidator INTERPAYMENT_VALIDATOR;
    public static final CodeValidator UATP_VALIDATOR;

    /**
     * Create a new CreditCardValidator with default options.
     */
    public CreditCardValidator() {
        this(AMEX + VISA + MASTERCARD + DISCOVER + MAESTRO +
                JCB + UNIONPAY + MIR + INTERPAYMENT + INSTAPAYMENT + UATP
        );
    }

    /**
     * Create a new CreditCardValidator with the specified options.
     * @param options Pass in
     * CreditCardValidator.VISA + CreditCardValidator.AMEX to specify that
     * those are the only valid card types.
     */
    public CreditCardValidator(long options) {
        this.cardTypes = new ArrayList();
        if (this.isOn(options, 2L)) {
            this.cardTypes.add(VISA_VALIDATOR);
        }

        if (this.isOn(options, 1L)) {
            this.cardTypes.add(AMEX_VALIDATOR);
        }

        if (this.isOn(options, 4L)) {
            this.cardTypes.add(MASTERCARD_VALIDATOR);
        }

        if (this.isOn(options, 8L)) {
            this.cardTypes.add(DISCOVER_VALIDATOR);
        }

        if (this.isOn(options, 16L)) {
            this.cardTypes.add(DINERS_VALIDATOR);
        }

        if (this.isOn(options, 64L)) {
            this.cardTypes.add(JCB_VALIDATOR);
        }

        if (this.isOn(options, 128L)) {
            this.cardTypes.add(UNIONPAY_VALIDATOR);
        }

        if (this.isOn(options, 32L)) {
            this.cardTypes.add(MAESTRO_VALIDATOR);
        }

        if (this.isOn(options, 256L)) {
            this.cardTypes.add(MIR_VALIDATOR);
        }

        if (this.isOn(options, 512L)) {
            this.cardTypes.add(INTERPAYMENT_VALIDATOR);
        }

        if (this.isOn(options, 2048L)) {
            this.cardTypes.add(UATP_VALIDATOR);
        }

    }

    /**
     * Create a new CreditCardValidator with the specified {@link CodeValidator}s.
     * @param creditCardValidators Set of valid code validators
     */
    public CreditCardValidator(CodeValidator[] creditCardValidators) {
        this.cardTypes = new ArrayList();
        if (creditCardValidators == null) {
            throw new IllegalArgumentException("Card validators are missing");
        }
        for (int i = 0; i < creditCardValidators.length; i++) {
            cardTypes.add(creditCardValidators[i]);
        }
    }

    /**
     * Checks if the field is a valid credit card number.
     * @param card The card number to validate.
     * @return Whether the card number is valid.
     */
    public boolean isValid(String card) {
        if (card == null || card.length() == 0) {
            return false;
        }
        for (int i = 0; i < cardTypes.size(); i++) {
            CodeValidator type = (CodeValidator)cardTypes.get(i);
            if (type.isValid(card)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the field is a valid credit card number.
     * @param card The card number to validate.
     * @return The card number if valid or <code>null</code>
     * if invalid.
     */
    public Object validate(String card) {
        if (card == null || card.length() == 0) {
            return null;
        }
        Object result = null;
        for (int i = 0; i < cardTypes.size(); i++) {
            CodeValidator type = (CodeValidator)cardTypes.get(i);
            result = type.validate(card);
            if (result != null) {
                return result ;
            }
        }
        return null;

    }
    /**
     * Tests whether the given flag is on.  If the flag is not a power of 2
     * (ie. 3) this tests whether the combination of flags is on.
     *
     * @param options The options specified.
     * @param flag Flag value to check.
     *
     * @return whether the specified flag value is on.
     */
    private boolean isOn(long options, long flag) {
        return (options & flag) > 0;
    }

    static {
        AMEX_VALIDATOR = new CodeValidator(AMEX_REGEX, LUHN_VALIDATOR);
        VISA_VALIDATOR = new CodeValidator(VISA_REGEX, LUHN_VALIDATOR);
        MASTERCARD_VALIDATOR = new CodeValidator(MASTERCARD_REGEX, LUHN_VALIDATOR);
        DISCOVER_VALIDATOR = new CodeValidator(DISCOVER_REGEX, LUHN_VALIDATOR);
        DINERS_VALIDATOR = new CodeValidator(DINERS_REGEX, LUHN_VALIDATOR);
        MAESTRO_VALIDATOR = new CodeValidator(MAESTRO_REGEX, LUHN_VALIDATOR);
        JCB_VALIDATOR = new CodeValidator(JCB_REGEX, LUHN_VALIDATOR);
        UNIONPAY_VALIDATOR = new CodeValidator(UNIONPAY_REGEX, LUHN_VALIDATOR);
        MIR_VALIDATOR = new CodeValidator(MIR_REGEX, LUHN_VALIDATOR);
        INTERPAYMENT_VALIDATOR = new CodeValidator(INTERPAYMENT_REGEX, LUHN_VALIDATOR);
        UATP_VALIDATOR = new CodeValidator(UATP_REGEX, LUHN_VALIDATOR);
    }
}
