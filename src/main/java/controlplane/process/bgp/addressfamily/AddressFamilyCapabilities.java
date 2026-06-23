package controlplane.process.bgp.addressfamily;

import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;
import java.util.Objects;

public class AddressFamilyCapabilities {
    private static final String PROP_ADDITIONAL_PATHS_RECEIVE = "additionalPathsReceive";
    private static final String PROP_ADDITIONAL_PATHS_SELECT_ALL = "additionalPathsSelectAll";
    private static final String PROP_ADDITIONAL_PATHS_SEND = "additionalPathsSend";
    private static final String PROP_ADVERTISE_EXTERNAL = "advertiseExternal";
    private static final String PROP_ADVERTISE_INACTIVE = "advertiseInactive";
    private static final String PROP_ALLOW_LOCAL_AS_IN = "allowLocalAsIn";
    private static final String PROP_ALLOW_REMOTE_AS_OUT = "allowRemoteAsOut";
    private static final String PROP_SEND_COMMUNITY = "sendCommunity";
    private static final String PROP_SEND_EXTENDED_COMMUNITY = "sendExtendedCommunity";

    /**
     * <a href="https://www.rfc-editor.org/rfc/rfc7911">RFC-7911: Advertisement of Multiple Paths in BGP</a> <br>
     * <a href="https://support.huawei.com/enterprise/en/doc/EDOC1100058916/b697a787/configuring-bgp-add-path">BGP ADD-PATH</a> <br>
     * BGP ADD-PATH allows a router reflector (RR) to send two or more routes with the same prefix to a specified
     * IBGP peer. These routes can back up each other to load-balance traffic, which improves network reliability. <br>
     *
     * Configuration on RR: <br>
     * peer {<i>client-ip-address</i> | <i>client-group-name</i>} capability-advertise add-path send <br>
     * peer {<i>client-ip-address</i> | <i>client-group-name</i>} advertise add-path path-number <i>path-number</i> <br>
     * Configuration on RR-Client: <br>
     * peer {<i>rr-ip-address</i> | <i>rr-group-name</i>} capability-advertise add-path receive <br>
     */
    private final boolean _additionalPathsReceive;
    private final boolean _additionalPathsSelectAll;
    private final boolean _additionalPathsSend;
    private final boolean _advertiseExternal;
    private final boolean _advertiseInactive;
    private final boolean _allowLocalAsIn;
    private final boolean _allowRemoteAsOut;
    /**
     * If this is true, a bgp speaker will send community to its peer or peer group.
     * By default, a bgp speaker advertises no community to its peer or peer group.
     */
    private final boolean _sendCommunity;
    private final boolean _sendExtendedCommunity;

    private AddressFamilyCapabilities(
            boolean additionalPathsReceive,
            boolean additionalPathsSelectAll,
            boolean additionalPathsSend,
            boolean advertiseExternal,
            boolean advertiseInactive,
            boolean allowLocalAsIn,
            boolean allowRemoteAsOut,
            boolean sendCommunity,
            boolean sendExtendedCommunity) {
        _additionalPathsReceive = additionalPathsReceive;
        _additionalPathsSelectAll = additionalPathsSelectAll;
        _additionalPathsSend = additionalPathsSend;
        _advertiseExternal = advertiseExternal;
        _advertiseInactive = advertiseInactive;
        _allowLocalAsIn = allowLocalAsIn;
        _allowRemoteAsOut = allowRemoteAsOut;
        _sendCommunity = sendCommunity;
        _sendExtendedCommunity = sendExtendedCommunity;
    }

    public boolean isAdditionalPathsReceive() {
        return _additionalPathsReceive;
    }

    public boolean isAdditionalPathsSend() {
        return _additionalPathsSend;
    }

    public boolean isSendCommunity() {
        return _sendCommunity;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AddressFamilyCapabilities)) {
            return false;
        }
        AddressFamilyCapabilities that = (AddressFamilyCapabilities) o;
        return _additionalPathsReceive == that._additionalPathsReceive
                && _additionalPathsSelectAll == that._additionalPathsSelectAll
                && _additionalPathsSend == that._additionalPathsSend
                && _advertiseExternal == that._advertiseExternal
                && _advertiseInactive == that._advertiseInactive
                && _allowLocalAsIn == that._allowLocalAsIn
                && _allowRemoteAsOut == that._allowRemoteAsOut
                && _sendCommunity == that._sendCommunity
                && _sendExtendedCommunity == that._sendExtendedCommunity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                _additionalPathsReceive,
                _additionalPathsSelectAll,
                _additionalPathsSend,
                _advertiseExternal,
                _advertiseInactive,
                _allowLocalAsIn,
                _allowRemoteAsOut,
                _sendCommunity,
                _sendExtendedCommunity);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add(PROP_ADDITIONAL_PATHS_RECEIVE, _additionalPathsReceive)
                .add(PROP_ADDITIONAL_PATHS_SELECT_ALL, _additionalPathsSelectAll)
                .add(PROP_ADDITIONAL_PATHS_SEND, _additionalPathsSend)
                .add(PROP_ADVERTISE_EXTERNAL, _advertiseExternal)
                .add(PROP_ADVERTISE_INACTIVE, _advertiseInactive)
                .add(PROP_ALLOW_LOCAL_AS_IN, _allowLocalAsIn)
                .add(PROP_ALLOW_REMOTE_AS_OUT, _allowRemoteAsOut)
                .add(PROP_SEND_COMMUNITY, _sendCommunity)
                .add(PROP_SEND_EXTENDED_COMMUNITY, _sendExtendedCommunity)
                .toString();
    }

    /**
     * Return a builder for {@link AddressFamilyCapabilities} By default all values are initialized to
     * {@code false}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean _additionalPathsReceive;
        private boolean _additionalPathsSelectAll;
        private boolean _additionalPathsSend;
        private boolean _advertiseExternal;
        private boolean _advertiseInactive;
        private boolean _allowLocalAsIn;
        private boolean _allowRemoteAsOut;
        private boolean _sendCommunity;
        private boolean _sendExtendedCommunity;

        private Builder() {}

        public Builder setAdditionalPathsReceive(boolean additionalPathsReceive) {
            _additionalPathsReceive = additionalPathsReceive;
            return this;
        }

        public Builder setAdditionalPathsSelectAll(boolean additionalPathsSelectAll) {
            _additionalPathsSelectAll = additionalPathsSelectAll;
            return this;
        }

        public Builder setAdditionalPathsSend(boolean additionalPathsSend) {
            _additionalPathsSend = additionalPathsSend;
            return this;
        }

        public Builder setAdvertiseExternal(boolean advertiseExternal) {
            _advertiseExternal = advertiseExternal;
            return this;
        }

        public Builder setAdvertiseInactive(boolean advertiseInactive) {
            _advertiseInactive = advertiseInactive;
            return this;
        }

        public Builder setAllowLocalAsIn(boolean allowLocalAsIn) {
            _allowLocalAsIn = allowLocalAsIn;
            return this;
        }

        public Builder setAllowRemoteAsOut(boolean allowRemoteAsOut) {
            _allowRemoteAsOut = allowRemoteAsOut;
            return this;
        }

        public Builder setSendCommunity(boolean sendCommunity) {
            _sendCommunity = sendCommunity;
            return this;
        }

        public Builder setSendExtendedCommunity(boolean sendExtendedCommunity) {
            _sendExtendedCommunity = sendExtendedCommunity;
            return this;
        }

        public AddressFamilyCapabilities build() {
            return new AddressFamilyCapabilities(
                    _additionalPathsReceive,
                    _additionalPathsSelectAll,
                    _additionalPathsSend,
                    _advertiseExternal,
                    _advertiseInactive,
                    _allowLocalAsIn,
                    _allowRemoteAsOut,
                    _sendCommunity,
                    _sendExtendedCommunity);
        }
    }
}
