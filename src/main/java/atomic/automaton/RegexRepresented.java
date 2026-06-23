package atomic.automaton;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** A representation of a regular expression for symbolic route analysis. */
@ParametersAreNonnullByDefault
public abstract class RegexRepresented implements AutomatonRepresented {
    @Nonnull protected final String _regex;

    public RegexRepresented(String regex) {
        _regex = toAutomatonRegex(regex);
    }

    @Nonnull
    public String getRegex() {
        return _regex;
    }

    // modify the given regex to conform to the grammar of the Automaton library that we use to
    // analyze regexes
    @Nonnull
    private String toAutomatonRegex(String regex) {
        // the Automaton library does not support the character class \d, which matches [0-9]
        return regex.replace("\\d", "[0-9]");
    }
}
