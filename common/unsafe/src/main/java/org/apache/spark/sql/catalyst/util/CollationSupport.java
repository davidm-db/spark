/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.catalyst.util;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.StringSearch;
import com.ibm.icu.util.ULocale;

import org.apache.spark.unsafe.types.UTF8String;

import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.Set;

/**
 * Static entry point for collation-aware expressions (StringExpressions, RegexpExpressions, and
 * other expressions that require custom collation support), as well as private utility methods for
 * collation-aware UTF8String operations needed to implement .
 */
public final class CollationSupport {

  /**
   * Collation-aware string expressions.
   */

  public static class Contains {
    public static boolean exec(final UTF8String l, final UTF8String r, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(l, r);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(l, r);
      } else {
        return execICU(l, r, collationId);
      }
    }
    public static String genCode(final String l, final String r, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.Contains.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", l, r);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", l, r);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", l, r, collationId);
      }
    }
    public static boolean execBinary(final UTF8String l, final UTF8String r) {
      return l.contains(r);
    }
    public static boolean execLowercase(final UTF8String l, final UTF8String r) {
      return l.containsInLowerCase(r);
    }
    public static boolean execICU(final UTF8String l, final UTF8String r,
        final int collationId) {
      if (r.numBytes() == 0) return true;
      if (l.numBytes() == 0) return false;
      StringSearch stringSearch = CollationFactory.getStringSearch(l, r, collationId);
      return stringSearch.first() != StringSearch.DONE;
    }
  }

  public static class StartsWith {
    public static boolean exec(final UTF8String l, final UTF8String r,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(l, r);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(l, r);
      } else {
        return execICU(l, r, collationId);
      }
    }
    public static String genCode(final String l, final String r, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StartsWith.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", l, r);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", l, r);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", l, r, collationId);
      }
    }
    public static boolean execBinary(final UTF8String l, final UTF8String r) {
      return l.startsWith(r);
    }
    public static boolean execLowercase(final UTF8String l, final UTF8String r) {
      return l.startsWithInLowerCase(r);
    }
    public static boolean execICU(final UTF8String l, final UTF8String r,
        final int collationId) {
      if (r.numBytes() == 0) return true;
      if (l.numBytes() == 0) return false;
      StringSearch stringSearch = CollationFactory.getStringSearch(l, r, collationId);
      return stringSearch.first() == 0;
    }
  }

  public static class EndsWith {
    public static boolean exec(final UTF8String l, final UTF8String r, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(l, r);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(l, r);
      } else {
        return execICU(l, r, collationId);
      }
    }
    public static String genCode(final String l, final String r, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.EndsWith.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", l, r);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", l, r);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", l, r, collationId);
      }
    }
    public static boolean execBinary(final UTF8String l, final UTF8String r) {
      return l.endsWith(r);
    }
    public static boolean execLowercase(final UTF8String l, final UTF8String r) {
      return l.endsWithInLowerCase(r);
    }
    public static boolean execICU(final UTF8String l, final UTF8String r,
        final int collationId) {
      if (r.numBytes() == 0) return true;
      if (l.numBytes() == 0) return false;
      StringSearch stringSearch = CollationFactory.getStringSearch(l, r, collationId);
      int endIndex = stringSearch.getTarget().getEndIndex();
      return stringSearch.last() == endIndex - stringSearch.getMatchLength();
    }
  }

  public static class Upper {
    public static UTF8String exec(final UTF8String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return execUTF8(v);
      } else {
        return execICU(v, collationId);
      }
    }
    public static String genCode(final String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.Upper.exec";
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return String.format(expr + "UTF8(%s)", v);
      } else {
        return String.format(expr + "ICU(%s, %d)", v, collationId);
      }
    }
    public static UTF8String execUTF8(final UTF8String v) {
      return v.toUpperCase();
    }
    public static UTF8String execICU(final UTF8String v, final int collationId) {
      return UTF8String.fromString(CollationAwareUTF8String.toUpperCase(v.toString(), collationId));
    }
  }

  public static class Lower {
    public static UTF8String exec(final UTF8String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return execUTF8(v);
      } else {
        return execICU(v, collationId);
      }
    }
    public static String genCode(final String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
        String expr = "CollationSupport.Lower.exec";
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return String.format(expr + "UTF8(%s)", v);
      } else {
        return String.format(expr + "ICU(%s, %d)", v, collationId);
      }
    }
    public static UTF8String execUTF8(final UTF8String v) {
      return v.toLowerCase();
    }
    public static UTF8String execICU(final UTF8String v, final int collationId) {
      return UTF8String.fromString(CollationAwareUTF8String.toLowerCase(v.toString(), collationId));
    }
  }

  public static class InitCap {
    public static UTF8String exec(final UTF8String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return execUTF8(v);
      } else {
        return execICU(v, collationId);
      }
    }

    public static String genCode(final String v, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.InitCap.exec";
      if (collation.supportsBinaryEquality || collation.supportsLowercaseEquality) {
        return String.format(expr + "UTF8(%s)", v);
      } else {
        return String.format(expr + "ICU(%s, %d)", v, collationId);
      }
    }

    public static UTF8String execUTF8(final UTF8String v) {
      return v.toLowerCase().toTitleCase();
    }

    public static UTF8String execICU(final UTF8String v, final int collationId) {
      return UTF8String.fromString(
              CollationAwareUTF8String.toTitleCase(
                      CollationAwareUTF8String.toLowerCase(
                              v.toString(),
                              collationId
                      ),
                      collationId));
    }
  }

  public static class FindInSet {
    public static int exec(final UTF8String word, final UTF8String set, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(word, set);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(word, set);
      } else {
        return execICU(word, set, collationId);
      }
    }
    public static String genCode(final String word, final String set, final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.FindInSet.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", word, set);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", word, set);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", word, set, collationId);
      }
    }
    public static int execBinary(final UTF8String word, final UTF8String set) {
      return set.findInSet(word);
    }
    public static int execLowercase(final UTF8String word, final UTF8String set) {
      return set.toLowerCase().findInSet(word.toLowerCase());
    }
    public static int execICU(final UTF8String word, final UTF8String set,
                                  final int collationId) {
      return CollationAwareUTF8String.findInSet(word, set, collationId);
    }
  }

  public static class StringInstr {
    public static int exec(final UTF8String string, final UTF8String substring,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(string, substring);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(string, substring);
      } else {
        return execICU(string, substring, collationId);
      }
    }
    public static String genCode(final String string, final String substring,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringInstr.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", string, substring);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", string, substring);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", string, substring, collationId);
      }
    }
    public static int execBinary(final UTF8String string, final UTF8String substring) {
      return string.indexOf(substring, 0);
    }
    public static int execLowercase(final UTF8String string, final UTF8String substring) {
      return string.toLowerCase().indexOf(substring.toLowerCase(), 0);
    }
    public static int execICU(final UTF8String string, final UTF8String substring,
        final int collationId) {
      return CollationAwareUTF8String.indexOf(string, substring, 0, collationId);
    }
  }

  public static class StringTrim {
    public static UTF8String exec(
        final UTF8String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString);
      } else {
        return execICU(srcString, collationId);
      }
    }
    public static UTF8String exec(
        final UTF8String srcString,
        final UTF8String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString, trimString);
      } else {
        return execICU(srcString, trimString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrim.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s)", srcString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s)", srcString);
      } else {
        return String.format(expr + "ICU(%s, %d)", srcString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrim.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", srcString, trimString);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", srcString, trimString, collationId);
      }
    }
    public static UTF8String execBinary(
        final UTF8String srcString) {
      return srcString.trim();
    }
    public static UTF8String execBinary(
        final UTF8String srcString,
        final UTF8String trimString) {
      return srcString.trim(trimString);
    }
    public static UTF8String execLowercase(
        final UTF8String srcString) {
      return srcString.trim();
    }
    public static UTF8String execLowercase(
        final UTF8String srcString,
        final UTF8String trimString) {
      return CollationAwareUTF8String.lowercaseTrim(srcString, trimString);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        int collationId) {
      return CollationAwareUTF8String.trim(srcString, collationId);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      return CollationAwareUTF8String.trim(srcString, trimString, collationId);
    }
  }

  public static class StringTrimLeft {
    public static UTF8String exec(
        final UTF8String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString);
      } else {
        return execICU(srcString, collationId);
      }
    }
    public static UTF8String exec(
        final UTF8String srcString,
        final UTF8String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString, trimString);
      } else {
        return execICU(srcString, trimString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrimLeft.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s)", srcString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s)", srcString);
      } else {
        return String.format(expr + "ICU(%s, %d)", srcString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrimLeft.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", srcString, trimString);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", srcString, trimString, collationId);
      }
    }
    public static UTF8String execBinary(
        final UTF8String srcString) {
      return srcString.trimLeft();
    }
    public static UTF8String execBinary(
        final UTF8String srcString,
        final UTF8String trimString) {
      return srcString.trimLeft(trimString);
    }
    public static UTF8String execLowercase(
        final UTF8String srcString) {
      return srcString.trimLeft();
    }
    public static UTF8String execLowercase(
        final UTF8String srcString,
        final UTF8String trimString) {
      return CollationAwareUTF8String.lowercaseTrimLeft(srcString, trimString);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        int collationId) {
      return CollationAwareUTF8String.trimLeft(srcString, collationId);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      return CollationAwareUTF8String.trimLeft(srcString, trimString, collationId);
    }
  }

  public static class StringTrimRight {
    public static UTF8String exec(
        final UTF8String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString);
      } else {
        return execICU(srcString, collationId);
      }
    }
    public static UTF8String exec(
        final UTF8String srcString,
        final UTF8String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      if (collation.supportsBinaryEquality) {
        return execBinary(srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return execLowercase(srcString, trimString);
      } else {
        return execICU(srcString, trimString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrimRight.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s)", srcString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s)", srcString);
      } else {
        return String.format(expr + "ICU(%s, %d)", srcString, collationId);
      }
    }
    public static String genCode(
        final String srcString,
        final String trimString,
        final int collationId) {
      CollationFactory.Collation collation = CollationFactory.fetchCollation(collationId);
      String expr = "CollationSupport.StringTrimRight.exec";
      if (collation.supportsBinaryEquality) {
        return String.format(expr + "Binary(%s, %s)", srcString, trimString);
      } else if (collation.supportsLowercaseEquality) {
        return String.format(expr + "Lowercase(%s, %s)", srcString, trimString);
      } else {
        return String.format(expr + "ICU(%s, %s, %d)", srcString, trimString, collationId);
      }
    }
    public static UTF8String execBinary(
        final UTF8String srcString) {
      return srcString.trimRight();
    }
    public static UTF8String execBinary(
        final UTF8String srcString,
        final UTF8String trimString) {
      return srcString.trimRight(trimString);
    }
    public static UTF8String execLowercase(
        final UTF8String srcString) {
      return srcString.trimRight();
    }
    public static UTF8String execLowercase(
        final UTF8String srcString,
        final UTF8String trimString) {
      return CollationAwareUTF8String.lowercaseTrimRight(srcString, trimString);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        int collationId) {
      return CollationAwareUTF8String.trimRight(srcString, collationId);
    }
    public static UTF8String execICU(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      return CollationAwareUTF8String.trimRight(srcString, trimString, collationId);
    }
  }

  // TODO: Add more collation-aware string expressions.

  /**
   * Collation-aware regexp expressions.
   */

  public static boolean supportsLowercaseRegex(final int collationId) {
    // for regex, only Unicode case-insensitive matching is possible,
    // so UTF8_BINARY_LCASE is treated as UNICODE_CI in this context
    return CollationFactory.fetchCollation(collationId).supportsLowercaseEquality;
  }

  private static final int lowercaseRegexFlags = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
  public static int collationAwareRegexFlags(final int collationId) {
    return supportsLowercaseRegex(collationId) ? lowercaseRegexFlags : 0;
  }

  private static final UTF8String lowercaseRegexPrefix = UTF8String.fromString("(?ui)");
  public static UTF8String lowercaseRegex(final UTF8String regex) {
    return UTF8String.concat(lowercaseRegexPrefix, regex);
  }
  public static UTF8String collationAwareRegex(final UTF8String regex, final int collationId) {
    return supportsLowercaseRegex(collationId) ? lowercaseRegex(regex) : regex;
  }

  /**
   * Other collation-aware expressions.
   */

  // TODO: Add other collation-aware expressions.

  /**
   * Utility class for collation-aware UTF8String operations.
   */

  private static class CollationAwareUTF8String {

    private static String toUpperCase(final String target, final int collationId) {
      ULocale locale = CollationFactory.fetchCollation(collationId)
              .collator.getLocale(ULocale.ACTUAL_LOCALE);
      return UCharacter.toUpperCase(locale, target);
    }

    private static String toLowerCase(final String target, final int collationId) {
      ULocale locale = CollationFactory.fetchCollation(collationId)
              .collator.getLocale(ULocale.ACTUAL_LOCALE);
      return UCharacter.toLowerCase(locale, target);
    }

    private static String toTitleCase(final String target, final int collationId) {
      ULocale locale = CollationFactory.fetchCollation(collationId)
              .collator.getLocale(ULocale.ACTUAL_LOCALE);
      return UCharacter.toTitleCase(locale, target, BreakIterator.getWordInstance(locale));
    }

    private static int findInSet(final UTF8String match, final UTF8String set, int collationId) {
      if (match.contains(UTF8String.fromString(","))) {
        return 0;
      }

      String setString = set.toString();
      StringSearch stringSearch = CollationFactory.getStringSearch(setString, match.toString(),
        collationId);

      int wordStart = 0;
      while ((wordStart = stringSearch.next()) != StringSearch.DONE) {
        boolean isValidStart = wordStart == 0 || setString.charAt(wordStart - 1) == ',';
        boolean isValidEnd = wordStart + stringSearch.getMatchLength() == setString.length()
                || setString.charAt(wordStart + stringSearch.getMatchLength()) == ',';

        if (isValidStart && isValidEnd) {
          int pos = 0;
          for (int i = 0; i < setString.length() && i < wordStart; i++) {
            if (setString.charAt(i) == ',') {
              pos++;
            }
          }

          return pos + 1;
        }
      }

      return 0;
    }

    private static int indexOf(final UTF8String target, final UTF8String pattern,
        final int start, final int collationId) {
      if (pattern.numBytes() == 0) {
        return 0;
      }

      StringSearch stringSearch = CollationFactory.getStringSearch(target, pattern, collationId);
      stringSearch.setIndex(start);

      return stringSearch.next();
    }

    private static UTF8String lowercaseTrim(
        final UTF8String srcString,
        final UTF8String trimString) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      UTF8String leftTrimmed = lowercaseTrimLeft(srcString, trimString);
      return lowercaseTrimRight(leftTrimmed, trimString);
    }

    private static UTF8String lowercaseTrimLeft(
        final UTF8String srcString,
        final UTF8String trimString) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      // The searching byte position in the srcString.
      int searchIdx = 0;
      // The byte position of a first non-matching character in the srcString.
      int trimByteIdx = 0;
      // Number of bytes in srcString.
      int numBytes = srcString.numBytes();
      // Convert trimString to lowercase so it can be searched properly.
      UTF8String lowercaseTrimString = trimString.toLowerCase();

      while (searchIdx < numBytes) {
        UTF8String searchChar = srcString.copyUTF8String(
          searchIdx,
          searchIdx + UTF8String.numBytesForFirstByte(srcString.getByte(searchIdx)) - 1);
        int searchCharBytes = searchChar.numBytes();

        // Try to find the matching for the searchChar in the trimString.
        if (lowercaseTrimString.find(searchChar.toLowerCase(), 0) >= 0) {
          trimByteIdx += searchCharBytes;
          searchIdx += searchCharBytes;
        } else {
          // No matching, exit the search.
          break;
        }
      }

      if (searchIdx == 0) {
        // Nothing trimmed - return original string (not converted to lowercase).
        return srcString;
      }
      if (trimByteIdx >= numBytes) {
        // Everything trimmed.
        return UTF8String.EMPTY_UTF8;
      }
      return srcString.copyUTF8String(trimByteIdx, numBytes - 1);
    }

    private static UTF8String lowercaseTrimRight(
        final UTF8String srcString,
        final UTF8String trimString) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      // Number of bytes iterated from the srcString.
      int byteIdx = 0;
      // Number of characters iterated from the srcString.
      int numChars = 0;
      // Number of bytes in srcString.
      int numBytes = srcString.numBytes();
      // Array of character length for the srcString.
      int[] stringCharLen = new int[numBytes];
      // Array of the first byte position for each character in the srcString
      int[] stringCharPos = new int[numBytes];
      // Non-final value for trim string to use.
      UTF8String lowercaseTrimString = trimString.toLowerCase();

      // Build the position and length array.
      while (byteIdx < numBytes) {
        stringCharPos[numChars] = byteIdx;
        stringCharLen[numChars] = UTF8String.numBytesForFirstByte(srcString.getByte(byteIdx));
        byteIdx += stringCharLen[numChars];
        numChars++;
      }

      // Index trimEnd points to the first no matching byte position from the right side of
      //  the source string.
      int trimByteIdx = numBytes - 1;

      while (numChars > 0) {
        UTF8String searchChar = srcString.copyUTF8String(
          stringCharPos[numChars - 1],
          stringCharPos[numChars - 1] + stringCharLen[numChars - 1] - 1);

        if(lowercaseTrimString.find(searchChar.toLowerCase(), 0) >= 0) {
          trimByteIdx -= stringCharLen[numChars - 1];
          numChars--;
        } else {
          break;
        }
      }

      if (trimByteIdx == numBytes - 1) {
        // Nothing trimmed.
        return srcString;
      }
      if (trimByteIdx < 0) {
        // Everything trimmed.
        return UTF8String.EMPTY_UTF8;
      }
      return srcString.copyUTF8String(0, trimByteIdx);
    }

    private static UTF8String trim(
        final UTF8String srcString,
        int collationId) {
      UTF8String leftTrimmed = trimLeft(srcString, collationId);
      return trimRight(leftTrimmed, collationId);
    }

    private static UTF8String trim(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      UTF8String leftTrimmed = trimLeft(srcString, trimString, collationId);
      return trimRight(leftTrimmed, trimString, collationId);
    }

    private static UTF8String trimLeft(
        final UTF8String srcString,
        int collationId) {
      return trimLeft(srcString, UTF8String.fromString(" "), collationId);
    }

    private static UTF8String trimLeft(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      // The searching byte position in the srcString.
      int searchIdx = 0;
      // The byte position of a first non-matching character in the srcString.
      int trimByteIdx = 0;
      // Number of bytes in srcString.
      int numBytes = srcString.numBytes();

      // Create ICU StringSearch object.
      StringSearch stringSearch = CollationFactory.getStringSearch(
        trimString, UTF8String.EMPTY_UTF8, collationId);
      // Create hash set to save seen chars
      Set<UTF8String> trimmedChars = new HashSet<>();

      while (searchIdx < numBytes) {
        UTF8String searchChar = srcString.copyUTF8String(
          searchIdx,
          searchIdx + UTF8String.numBytesForFirstByte(srcString.getByte(searchIdx)) - 1);
        int searchCharBytes = searchChar.numBytes();

        // First check if we have already seen this char in srcString.
        if (trimmedChars.contains(searchChar)) {
          trimByteIdx += searchCharBytes;
          searchIdx += searchCharBytes;
          continue;
        }

        // Otherwise, try to find the matching for the searchChar in the trimString.
        stringSearch.reset();
        stringSearch.setPattern(searchChar.toString());
        int searchCharIdx = stringSearch.next();

        if (searchCharIdx != StringSearch.DONE
            && stringSearch.getMatchLength() == stringSearch.getPattern().length()) {
          trimByteIdx += searchCharBytes;
          searchIdx += searchCharBytes;
          trimmedChars.add(searchChar);
        } else {
          // No matching, exit the search.
          break;
        }
      }

      if (searchIdx == 0) {
        // Nothing trimmed - return original string (not converted to lowercase).
        return srcString;
      }
      if (trimByteIdx >= numBytes) {
        // Everything trimmed.
        return UTF8String.EMPTY_UTF8;
      }
      return srcString.copyUTF8String(trimByteIdx, numBytes - 1);
    }

    private static UTF8String trimRight(
        final UTF8String srcString,
        int collationId) {
      return trimRight(srcString, UTF8String.fromString(" "), collationId);
    }

    private static UTF8String trimRight(
        final UTF8String srcString,
        final UTF8String trimString,
        int collationId) {
      // Matching UTF8String behavior for null `trimString`.
      if (trimString == null) {
        return null;
      }

      // Number of bytes iterated from the srcString.
      int byteIdx = 0;
      // Number of characters iterated from the srcString.
      int numChars = 0;
      // Number of bytes in srcString.
      int numBytes = srcString.numBytes();
      // Array of character length for the srcString.
      int[] stringCharLen = new int[numBytes];
      // Array of the first byte position for each character in the srcString.
      int[] stringCharPos = new int[numBytes];

      // Build the position and length array.
      while (byteIdx < numBytes) {
        stringCharPos[numChars] = byteIdx;
        stringCharLen[numChars] = UTF8String.numBytesForFirstByte(srcString.getByte(byteIdx));
        byteIdx += stringCharLen[numChars];
        numChars++;
      }

      // Create ICU StringSearch object.
      StringSearch stringSearch = CollationFactory.getStringSearch(
        trimString, UTF8String.EMPTY_UTF8, collationId);
      // Create hash set to save seen chars
      Set<UTF8String> trimmedChars = new HashSet<>();

      // Index trimEnd points to the first no matching byte position from the right side of
      //  the source string.
      int trimByteIdx = numBytes - 1;

      while (numChars > 0) {
        UTF8String searchChar = srcString.copyUTF8String(
          stringCharPos[numChars - 1],
          stringCharPos[numChars - 1] + stringCharLen[numChars - 1] - 1);

        // First check if we have already seen this char in srcString.
        if (trimmedChars.contains(searchChar)) {
          trimByteIdx -= stringCharLen[numChars - 1];
          numChars--;
          continue;
        }

        // Otherwise, try to find the matching for the searchChar in the trimString.
        stringSearch.reset();
        stringSearch.setPattern(searchChar.toString());
        int searchCharIdx = stringSearch.next();

        if (searchCharIdx != StringSearch.DONE
            && stringSearch.getMatchLength() == stringSearch.getPattern().length()) {
          trimByteIdx -= stringCharLen[numChars - 1];
          numChars--;
          trimmedChars.add(searchChar);
        } else {
          break;
        }
      }

      if (trimByteIdx == numBytes - 1) {
        // Nothing trimmed.
        return srcString;
      }
      if (trimByteIdx < 0) {
        // Everything trimmed.
        return UTF8String.EMPTY_UTF8;
      }
      return srcString.copyUTF8String(0, trimByteIdx);
    }

  }

}
