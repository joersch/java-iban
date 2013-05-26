/*
   Copyright 2013 Barend Garvelink

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.garvelink.iban;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Miscellaneous tests for the {@link IBAN} class.
 */
public class IBANTest {
    private static final String VALID_IBAN = "NL91ABNA0417164300";
    private static final String INVALID_IBAN = "NL12ABNA0417164300";

    @Test
    public void getCountryCodeShouldReturnTheCountryCode() {
        assertThat(IBAN.parse(VALID_IBAN).getCountryCode(), is("NL"));
    }

    @Test
    public void getCheckDigitsShouldReturnTheCheckDigits() {
        assertThat(IBAN.parse(VALID_IBAN).getCheckDigits(), is("91"));
    }

    @Test
    public void valueOfNullIsNull() {
        assertThat(IBAN.valueOf(null), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseShouldRejectNull() {
        IBAN.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseShouldRejectInvalidInput() {
        IBAN.parse("Shenanigans!");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseShouldRejectLeadingWhitespace() {
        IBAN.parse(" " + VALID_IBAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseShouldRejectTrailingWhitespace() {
        IBAN.parse(VALID_IBAN + ' ');
    }

    @Test(expected = UnknownCountryCodeException.class)
    public void parseShouldRejectUnknownCountryCode() {
        IBAN.parse("UU345678345543234");
    }

    @Test
    public void parseShouldRejectChecksumFailure() {
        try {
            IBAN.parse(INVALID_IBAN);
            fail("Invalid input should have been rejected for checksum mismatch.");
        } catch (WrongChecksumException e) {
            assertThat(e.getFailedInput(), is(INVALID_IBAN));
        }
    }

    @Test
    public void testEqualsContract() {
        IBAN x = IBAN.parse(VALID_IBAN);
        IBAN y = IBAN.parse(VALID_IBAN);
        IBAN z = IBAN.parse(VALID_IBAN);

        assertFalse("No object equals null", x.equals(null));
        assertTrue("An object equals itself", x.equals(x));
        assertTrue("Equality is symmetric", x.equals(y) && y.equals(x));
        assertTrue("Equality is transitive", x.equals(y) && y.equals(z) && x.equals(z));
        assertEquals("Equal objects have the same hash code", x.hashCode(), y.hashCode());
    }

    @Test
    public void testInvalidInputToGetLengthForCountry() {
        assertThat(IBAN.getLengthForCountryCode("nl"), is(-1));
        assertThat(IBAN.getLengthForCountryCode("Bogus"), is(-1));
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        IBAN source = IBAN.parse(VALID_IBAN);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(source);
        byte[] serializedForm = baos.toByteArray();
        System.out.printf("Length of serialized form: %d (%.2f%% length of IBAN)\n", serializedForm.length, 100.0f * serializedForm.length / VALID_IBAN.length());
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedForm);
        ObjectInputStream ois = new ObjectInputStream(bais);
        IBAN clone = (IBAN) ois.readObject();
        assertThat(clone, is(equalTo(source)));
        // The pretty print isn't part of the serialized form, make sure it doesn't nullpointer or anything.
        assertThat(clone.toString(), is(equalTo(source.toString())));
    }

    /**
     * Deserializes a stored copy of the serialized form, to protect against breakage due to future changes.
     */
    @Test
    public void testSerializationCompatibility() throws IOException, ClassNotFoundException {
        InputStream is = IBANTest.class.getResourceAsStream("/IBAN.ser");
        ObjectInputStream ois = new ObjectInputStream(is);
        IBAN clone = (IBAN) ois.readObject();
        assertThat(clone, is(equalTo(IBAN.valueOf(VALID_IBAN))));
    }

    /**
     * Deserializes an invalid serialized form, to verify that instance validation occurs.
     */
    @Test(expected = InvalidObjectException.class)
    public void testSerializationEnsuresIntegrity() throws IOException, ClassNotFoundException {
        InputStream is = IBANTest.class.getResourceAsStream("/IBAN-invalid.ser");
        ObjectInputStream ois = new ObjectInputStream(is);
        IBAN clone = (IBAN) ois.readObject();
    }
}
