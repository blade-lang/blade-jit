package org.blade;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;

import java.util.regex.Pattern;

/**
 * Tests if the argument is a string that matches a regex.
 */
public class BStringMatches extends SubstringMatcher {
  public BStringMatches(String substring) {
    super(substring);
  }

  @Override
  protected boolean evalSubstringOf(String s) {
    return Pattern.compile(substring, Pattern.MULTILINE | Pattern.DOTALL)
      .matcher(s)
      .find();
  }

  @Override
  protected String relationship() {
    return "that matches";
  }

  /**
   * Creates a matcher that matches if the examined {@link String} matches the regex specified
   * {@link String}.
   * <p/>
   * For example:
   * <pre>assertThat("myStringOfNote", matchesAs("\\d+"))</pre>
   *
   * @param regex
   *      the regex that the returned matcher will expect to match the examined string
   */
  @Factory
  public static Matcher<String> matchesAs(String regex) {
    return new BStringMatches(regex);
  }

}
