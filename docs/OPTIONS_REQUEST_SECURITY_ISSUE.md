# WSO2 Identity Server - OPTIONS Request Security Configuration Issue

## Overview

This document describes a known issue with dynamic API resource access control configuration in WSO2 Identity Server 7.2 M5, where OPTIONS requests are incorrectly treated as secured when using deployment.toml resource access control configurations.

## Problem Statement

When configuring API resource access control in `deployment.toml` using the following pattern:

```toml
[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "all"
```

OPTIONS requests to the configured endpoints require authentication, which:
- Breaks CORS (Cross-Origin Resource Sharing) preflight requests
- Violates HTTP specifications for OPTIONS method handling
- Prevents proper API discovery and documentation tools from working

## Impact

### Affected Scenarios
1. **Web Applications**: CORS preflight requests fail
2. **API Documentation**: Tools like Swagger UI cannot discover API methods
3. **Development Tools**: REST clients may fail to retrieve allowed methods
4. **Mobile Applications**: Cross-origin requests are blocked

### Symptoms
- OPTIONS requests return 401 Unauthorized instead of 200/204
- CORS errors in browser developer console
- API documentation tools unable to load endpoint information
- Cross-origin requests failing at preflight stage

## Root Cause

The issue originates in the `carbon-identity-framework` repository in the resource access control template processing logic. Dynamic configurations from `deployment.toml` are processed before the global OPTIONS exemption rule, causing them to override the intended behavior.

**Affected File**: `resource-access-control-v2.xml.j2` in carbon-identity-framework
**Issue**: Processing order of configuration rules

## Workarounds

Until the issue is fixed in carbon-identity-framework, consider these workarounds:

### 1. Specific HTTP Method Configuration
Instead of using `http_method = "all"`, specify individual methods:

```toml
[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "GET"

[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "POST"

[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "PUT"

[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "DELETE"

# Note: Deliberately exclude OPTIONS method
```

### 2. Proxy/Gateway Level Security
Configure security at the proxy or API gateway level instead of at the Identity Server level.

### 3. Application-Level CORS Handling
Handle CORS in your application rather than relying on browser preflight requests.

## Testing the Issue

A test case has been created to reproduce and validate this issue:

**Location**: `modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/identity/integration/test/resource/access/control/OptionsRequestSecurityTestCase.java`

**Test Configuration**: `modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/resource-access-control/options-request-test.toml`

## Expected Resolution

The fix requires changes in the carbon-identity-framework repository:

1. **Move dynamic configuration processing** after the OPTIONS exemption rule
2. **Ensure OPTIONS requests are always unsecured** regardless of dynamic configurations
3. **Maintain backward compatibility** for existing configurations

## Version Information

- **Affected Version**: WSO2 Identity Server 7.2 M5
- **Component**: carbon-identity-framework (dependency)
- **Area**: Identity Server Core

## Configuration Best Practices

### ✅ Recommended
```toml
# Specify exact HTTP methods instead of "all"
[[resource.access_control]]
context = "(.*/api/specific/endpoint.*)"
secure = true
http_method = "GET"
```

### ❌ Problematic
```toml
# Avoid using "all" as it affects OPTIONS requests
[[resource.access_control]]
context = "(.*/api/specific/endpoint.*)"
secure = true
http_method = "all"
```

## Additional Resources

- [HTTP OPTIONS Method RFC 7231](https://tools.ietf.org/html/rfc7231#section-4.3.7)
- [CORS Specification](https://www.w3.org/TR/cors/)
- [WSO2 Identity Server Documentation](https://is.docs.wso2.com/)

## Support

For questions or issues related to this problem:
1. Check the test cases in this repository
2. Review the workarounds provided above
3. Monitor the carbon-identity-framework repository for fixes
4. Contact WSO2 Support if using a supported version

---

*This document will be updated once the issue is resolved in the upstream carbon-identity-framework repository.*