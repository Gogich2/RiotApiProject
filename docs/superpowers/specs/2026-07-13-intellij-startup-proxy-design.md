# IntelliJ Startup Proxy Fix

**Date:** 2026-07-13

**Status:** Approved design

## Goal

Allow the Spring Boot application to start from IntelliJ IDEA without failing
while creating the champion-build repository beans.

## Root Cause

Spring Boot currently selects class-based CGLIB proxies. The champion-build
module has three `final` classes annotated with `@Repository`, so Spring's
exception-translation advisor cannot subclass them. Startup stops at the first
one, `JdbcBuildAssetRepository`; changing only that class would expose the same
failure in the remaining final repository classes.

## Decision

Set `spring.aop.proxy-target-class=false` in `application.properties`.
Spring will then proxy the existing repository interfaces. This preserves the
final implementation classes, keeps exception translation, and fixes all three
repositories with one configuration change.

No repository annotations, interfaces, or class modifiers will change.

## Testing

Add a focused Spring context regression test that loads the real application
configuration, registers a final repository implementation, and verifies that
the bean can be created through its interface. The test must fail with the
current CGLIB setting and pass after the property change.

After the focused and full unit suites pass, launch the real application from
the repository root and verify that startup proceeds beyond repository proxy
creation and reaches a listening HTTP port. Any later database or migration
failure will be reported separately rather than hidden by this fix.
