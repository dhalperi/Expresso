package datamodel;

import datamodel.route.RouteFilter;

/**
 * All implements of this interface includes {@link datamodel.aspath.AsPathRegex}, {@link
 * datamodel.community.CommunityRegex}, {@link datamodel.community.CommunityRegexConjunction},
 * {@link datamodel.ipv4.PrefixRange} {@link datamodel.tag.Tag}
 */
public interface CanBeMatched extends RouteFilter {}
