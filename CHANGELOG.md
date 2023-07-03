# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com)
and this project adheres to
[Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- This library has been renamed to `liberator.resource.discovery`.
- Rather than `handler` accepting an `options` map, it now accepts an
  `overrides` map, used as the most specific definition map when building the
  resource. As such, anything that would normally go into a liberator resource
  definition map can be provided.
- Rather than providing a `:links` option with details of the links to add to
  the discovery response, now a `:link-definitions` key should be provided in
  the `overrides` map, whose value can be:
    - a map of link names to link parameters
    - a vector of keyword route names
    - a vector of link parameters
    - a vector of functions of context producing link maps
- The default links have now been removed.

## 0.1.0 — 2020-03-30

Released without _CHANGELOG.md_.


[0.1.0]: https://github.com/logicblocks/liberator.resource.discovery/compare/9d16990eb05ce5137adca7a0f8cb15b463af616f...0.1.0
[Unreleased]: https://github.com/logicblocks/liberator.resource.discovery/compare/0.1.0...HEAD
