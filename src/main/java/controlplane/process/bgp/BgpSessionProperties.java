package controlplane.process.bgp;

import com.google.common.base.MoreObjects;

import static controlplane.process.bgp.addressfamily.AddressFamily.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class BgpSessionProperties {
    @Nonnull BgpPeerConfig local;
    BgpPeerConfig remote;

    Map<Type, RouteExchange> routeExchangeSettings;

    public BgpSessionProperties(
            @Nonnull BgpPeerConfig local,
            BgpPeerConfig remote) {
        this.local = local;
        this.remote = remote;
    }

    @Nonnull
    public BgpPeerConfig getLocal() {
        return local;
    }

    public BgpPeerConfig getRemote() {
        return remote;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        @Nonnull BgpPeerConfig local;
        BgpPeerConfig remote;

        private Builder() {
        }

        public BgpSessionProperties build() {
            return new BgpSessionProperties(
                    local,
                    remote
            );
        }

        public Builder setLocal(BgpPeerConfig local) {
            this.local = local;
            return this;
        }

        public Builder setRemote(BgpPeerConfig remote) {
            this.remote = remote;
            return this;
        }
    }

    public static final class RouteExchange {
        private final boolean _additionalPaths;
        private final boolean _advertiseExternal;
        private final boolean _advertiseInactive;

        RouteExchange(
                boolean additionalPaths,
                boolean advertiseExternal,
                boolean advertiseInactive) {
            _additionalPaths = additionalPaths;
            _advertiseExternal = advertiseExternal;
            _advertiseInactive = advertiseInactive;
        }

        /**
         * When this is set, add best eBGP path independently of whether it is preempted by an iBGP or
         * IGP route. Only applicable to iBGP sessions.
         */
        public boolean getAdvertiseExternal() {
            return _advertiseExternal;
        }

        /**
         * When this is true, add best BGP path independently of whether it is preempted by an IGP
         * route. Only applicable to eBGP sessions.
         */
        public boolean getAdvertiseInactive() {
            return _advertiseInactive;
        }

        /**
         * When this is true, advertise all paths from the multipath RIB
         */
        public boolean getAdditionalPaths() {
            return _additionalPaths;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RouteExchange)) {
                return false;
            }
            RouteExchange that = (RouteExchange) o;
            return _additionalPaths == that._additionalPaths
                    && _advertiseExternal == that._advertiseExternal
                    && _advertiseInactive == that._advertiseInactive;
        }

        @Override
        public int hashCode() {
            return Objects.hash(_additionalPaths, _advertiseExternal, _advertiseInactive);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("additionalPaths", _additionalPaths)
                    .add("advertiseExternal", _advertiseExternal)
                    .add("advertiseInactive", _advertiseInactive)
                    .toString();
        }
    }
}
