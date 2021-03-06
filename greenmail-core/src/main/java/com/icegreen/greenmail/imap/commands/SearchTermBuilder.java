package com.icegreen.greenmail.imap.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.*;

/**
 * Builder for search terms.
 *
 * @author mm
 */
public abstract class SearchTermBuilder {
    private SearchKey key;
    private List<String> parameters = Collections.<String>emptyList();

    public static SearchTermBuilder create(final String pTerm) {
        return create(SearchKey.valueOf(pTerm));
    }

    public static SearchTermBuilder create(final SearchKey key) {
        SearchTermBuilder builder;
        switch (key) {
            // Non flags first
            case HEADER:
                builder = createHeaderTermBuilder();
                break;
            // Flags
            case ALL:
                builder = createSearchTermBuilder(new AllSearchTerm());
                break;
            case ANSWERED:
                builder = createFlagSearchTermBuilder("ANSWERED", true);
                break;
            case BCC:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.BCC);
                break;
            case CC:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.CC);
                break;
            case DELETED:
                builder = createFlagSearchTermBuilder("DELETED", true);
                break;
            case DRAFT:
                builder = createFlagSearchTermBuilder("DRAFT", true);
                break;
            case FLAGGED:
                builder = createFlagSearchTermBuilder("FLAGGED", true);
                break;
            case FROM:
                builder = createFromSearchTermBuilder();
                break;
            case NEW:
                builder = createSearchTermBuilder(
                        new AndTerm(createFlagSearchTerm("RECENT", true), createFlagSearchTerm("SEEN", false))
                );
                break;
            case OLD:
                builder = createSearchTermBuilder(createFlagSearchTerm("RECENT", false));
                break;
            case RECENT:
                builder = createSearchTermBuilder(createFlagSearchTerm("RECENT", true));
                break;
            case SEEN:
                builder = createSearchTermBuilder(createFlagSearchTerm("SEEN", true));
                break;
            case TO:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.TO);
                break;
            case UNANSWERED:
                builder = createSearchTermBuilder(createFlagSearchTerm("ANSWERED", false));
                break;
            case UNDELETED:
                builder = createSearchTermBuilder(createFlagSearchTerm("DELETED", false));
                break;
            case UNDRAFT:
                builder = createSearchTermBuilder(createFlagSearchTerm("DRAFT", false));
                break;
            case UNFLAGGED:
                builder = createSearchTermBuilder(createFlagSearchTerm("FLAGGED", false));
                break;
            case UNSEEN:
                builder = createSearchTermBuilder(createFlagSearchTerm("SEEN", false));
                break;
            case KEYWORD:
            case UNKEYWORD:
                builder = createKeywordSearchTermBuilder(key);
                break;
            default:
                throw new IllegalStateException("Unsupported search term '" + key + '\'');
        }
        builder.setSearchKey(key);
        return builder;
    }

    private void setSearchKey(final SearchKey pKey) {
        key = pKey;
    }

    private static SearchTermBuilder createHeaderTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new HeaderTerm(getParameters().get(0), getParameters().get(1));
            }
        };
    }

    SearchTermBuilder addParameter(final String pParameter) {
        if (Collections.<String>emptyList() == parameters) {
            parameters = new ArrayList<String>();
        }
        parameters.add(pParameter);
        return this;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getParameter(final int pIdx) {
        return getParameters().get(pIdx);
    }

    public boolean expectsParameter() {
        return parameters.size() < key.getNumberOfParameters();
    }

    public abstract SearchTerm build();

    private static SearchTermBuilder createSearchTermBuilder(final SearchTerm pSearchTerm) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return pSearchTerm;
            }
        };
    }

    private static SearchTermBuilder createRecipientSearchTermBuilder(final Message.RecipientType type) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                try {
                    return new RecipientTerm(type, new InternetAddress(getParameters().get(0)));
                } catch (AddressException e) {
                    throw new IllegalArgumentException("Address is not correct", e);
                }
            }
        };
    }

    private static SearchTermBuilder createFromSearchTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                try {
                    return new FromTerm(new InternetAddress(getParameters().get(0)));
                } catch (AddressException e) {
                    throw new IllegalArgumentException("Address is not correct", e);
                }
            }
        };
    }

    private static SearchTermBuilder createFlagSearchTermBuilder(final String pFlagName, final boolean pValue) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return createFlagSearchTerm(pFlagName, pValue);
            }
        };
    }

    private static SearchTermBuilder createKeywordSearchTermBuilder(final SearchKey pKey) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return createFlagSearchTerm(getParameter(0), pKey == SearchKey.KEYWORD);
            }
        };
    }

    private static SearchTerm createFlagSearchTerm(String pFlagName, boolean pValue) {
        Flags.Flag flag = toFlag(pFlagName);
        Flags flags = new javax.mail.Flags();
        if (null == flag) { // user flags
            flags.add(pFlagName);
        } else {
            flags.add(flag);
        }
        return new FlagTerm(flags, pValue);
    }

    private static javax.mail.Flags.Flag toFlag(String pFlag) {
        if (pFlag == null || pFlag.trim().length() < 1) {
            throw new IllegalArgumentException("Can not convert empty string to mail flag");
        }
        String flag = pFlag.trim().toUpperCase();
        if (flag.equals("ANSWERED")) {
            return javax.mail.Flags.Flag.ANSWERED;
        }
        if (flag.equals("DELETED")) {
            return javax.mail.Flags.Flag.DELETED;
        }
        if (flag.equals("DRAFT")) {
            return javax.mail.Flags.Flag.DRAFT;
        }
        if (flag.equals("FLAGGED")) {
            return javax.mail.Flags.Flag.FLAGGED;
        }
        if (flag.equals("RECENT")) {
            return javax.mail.Flags.Flag.RECENT;
        }
        if (flag.equals("SEEN")) {
            return javax.mail.Flags.Flag.SEEN;
        }
        return null;
    }

    @Override
    public String toString() {
        return "SearchTermBuilder{" +
                "key=" + key +
                ", parameters=" + parameters +
                '}';
    }

    /**
     * Search term that matches all messages
     */
    private static class AllSearchTerm extends SearchTerm {
        @Override
        public boolean match(Message msg) {
            return true;
        }
    }
}
