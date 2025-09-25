# Issue Analysis: OPTIONS Authentication Deployment.toml Configuration

## Problem Statement

When adding dynamic API resource access control configurations in `deployment.toml` like:
```toml
[[resource.access_control]]
context = "(.)/scim2/Agents(.)"
secure="true"
http_method="all"
```

OPTIONS requests to the specified context are incorrectly being treated as secured, even though OPTIONS requests are explicitly marked as not secured in the carbon-identity-framework configuration template.

## Root Cause

The issue stems from the order of configuration processing in the `resource-access-control-v2.xml.j2` template file in the carbon-identity-framework repository:

1. Dynamic API resource access control configurations from deployment.toml are processed first (lines 21-41)
2. The OPTIONS exemption rule `<Resource context="(.*)" secured="false" http-method="OPTIONS"/>` comes after (around line 44)

Since the dynamic configurations are processed first and have `http_method="all"`, they match OPTIONS requests before the exemption rule can apply.

## Expected Behavior

OPTIONS requests should not be treated as secured, regardless of dynamic API resource configurations, as they are explicitly exempted in the framework configuration.

## Affected Components

- **Primary**: carbon-identity-framework repository
  - File: `features/identity-core/org.wso2.carbon.identity.core.server.feature/resources/resource-access-control-v2.xml.j2`
  - Lines: 21-41 (dynamic config processing) vs line 44 (OPTIONS exemption)

- **Secondary**: product-is repository (this repository)
  - Users configure API resources via deployment.toml
  - Test cases and documentation may need updates

## Suggested Solution

Move the dynamic API resource access control configuration processing (lines 21-41 in resource-access-control-v2.xml.j2) to come AFTER the OPTIONS exemption rule.

This ensures that:
1. OPTIONS requests are always treated as unsecured (as intended)
2. Dynamic configurations only apply to non-OPTIONS requests
3. Backward compatibility is maintained

## Repository Context

This issue is primarily in the carbon-identity-framework repository, but affects users of the product-is repository who configure API resources via deployment.toml.

## Test Case Needed

A test case should be created to verify that:
1. OPTIONS requests to dynamically configured API resources remain unsecured
2. Non-OPTIONS requests to the same resources are properly secured
3. The behavior is consistent across different API resource patterns

## Files in This Repository

The following files in this repository contain examples of resource.access_control configuration:
- `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/identity_new_resource.toml`
- `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/identity_new_resource_openjdknashorn.toml` 
- `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/identity_new_resource_nashorn.toml`

These show the proper format:
```toml
[[resource.access_control]]
context = "(.*)/sample-auth/(.*)"
secure = false
http_method = "all"
```

## Next Steps

1. Create integration test to reproduce the issue
2. Submit issue to carbon-identity-framework repository
3. Update documentation once fix is available