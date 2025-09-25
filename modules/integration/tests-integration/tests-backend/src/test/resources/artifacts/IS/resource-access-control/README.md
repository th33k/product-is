# OPTIONS Request Security Issue Test

This directory contains test cases and configuration files to reproduce and validate the fix for the OPTIONS request security issue in WSO2 Identity Server.

## Issue Description

When dynamic API resource access control configurations are added to `deployment.toml`, OPTIONS requests to those endpoints are incorrectly treated as secured, even though OPTIONS requests should always be unsecured according to the framework configuration.

### Example Configuration That Causes the Issue

```toml
[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "all"
```

With this configuration, OPTIONS requests to `/scim2/Agents` endpoints will require authentication, which breaks CORS preflight requests and violates HTTP specifications.

## Root Cause

The issue is in the carbon-identity-framework repository in the file:
`features/identity-core/org.wso2.carbon.identity.core.server.feature/resources/resource-access-control-v2.xml.j2`

The dynamic API resource configurations are processed before the OPTIONS exemption rule, causing the dynamic rules to override the OPTIONS exemption.

## Test Files

### Test Case
- `OptionsRequestSecurityTestCase.java` - Integration test that verifies OPTIONS requests remain unsecured

### Configuration
- `options-request-test.toml` - Deployment configuration that reproduces the issue

## Expected Behavior

1. **OPTIONS requests should NEVER be secured**, regardless of dynamic API resource configurations
2. **Non-OPTIONS requests should be secured** when configured in deployment.toml
3. **CORS preflight requests should work** without authentication

## Test Scenarios

The test case covers:

1. **OPTIONS to SCIM2 Agents endpoint** - Should return 200/204/405, not 401
2. **GET to SCIM2 Agents endpoint** - Should return 401 (secured as expected)
3. **CORS preflight OPTIONS request** - Should not require authentication

## Running the Test

```bash
mvn test -Dtest=OptionsRequestSecurityTestCase
```

## Current Failure

The test will currently **FAIL** because OPTIONS requests are incorrectly being secured. The test demonstrates the issue that needs to be fixed in the carbon-identity-framework repository.

## Expected Fix

Once the issue is fixed in carbon-identity-framework:
1. The dynamic API resource configuration processing should be moved after the OPTIONS exemption rule
2. OPTIONS requests should always be treated as unsecured
3. The test case should pass

## Files Structure

```
resource-access-control/
├── README.md (this file)
├── options-request-test.toml (test configuration)
└── ../java/.../OptionsRequestSecurityTestCase.java (test case)
```

## Related Issues

This issue affects:
- CORS preflight requests
- API documentation tools that use OPTIONS requests
- Web applications that perform CORS requests to WSO2 IS APIs

## HTTP Specification Compliance

According to RFC 7231, OPTIONS requests are used for:
- Cross-origin resource sharing (CORS) preflight checks
- Discovering allowed methods on a resource
- These should not require authentication for proper HTTP compliance