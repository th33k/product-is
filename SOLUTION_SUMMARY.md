# Summary: OPTIONS Request Authentication Issue

## Issue Description
When dynamic API resource access control is configured in `deployment.toml`, OPTIONS requests are incorrectly being secured, breaking CORS functionality and violating HTTP standards.

## Root Cause
The dynamic API resource access control configuration processing occurs before the OPTIONS exemption rule in the `resource-access-control-v2.xml.j2` template (carbon-identity-framework repository).

## Files Created/Modified

### 1. Issue Analysis
- **File**: `/workspaces/product-is/ISSUE_ANALYSIS.md`
- **Purpose**: Detailed technical analysis of the problem

### 2. Test Case
- **File**: `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/identity/integration/test/resource/access/control/OptionsRequestSecurityTestCase.java`
- **Purpose**: Integration test to reproduce and validate the issue
- **Tests**:
  - OPTIONS requests to secured SCIM2 Agents endpoint
  - GET requests to same endpoint (should be secured)
  - CORS preflight OPTIONS requests

### 3. Test Configuration
- **File**: `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/resource-access-control/options-request-test.toml`
- **Purpose**: Deployment configuration that demonstrates the issue

### 4. Test Documentation
- **File**: `/workspaces/product-is/modules/integration/tests-integration/tests-backend/src/test/resources/artifacts/IS/resource-access-control/README.md`
- **Purpose**: Explains the test setup and expected behavior

### 5. User Documentation
- **File**: `/workspaces/product-is/docs/OPTIONS_REQUEST_SECURITY_ISSUE.md`
- **Purpose**: User-facing documentation with workarounds and best practices

## Key Configuration Example

### Problematic Configuration (causes the issue):
```toml
[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "all"  # This affects OPTIONS requests
```

### Workaround Configuration:
```toml
# Specify individual methods instead of "all"
[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "GET"

[[resource.access_control]]
context = "(.*/scim2/Agents.*)"
secure = true
http_method = "POST"
# Note: OPTIONS is not included, so it remains unsecured
```

## Expected Test Results

### Current Behavior (Issue Present):
- OPTIONS requests return 401 Unauthorized ❌
- CORS preflight requests fail ❌
- API documentation tools cannot access endpoints ❌

### Expected Behavior (After Fix):
- OPTIONS requests return 200/204/405 ✅
- CORS preflight requests succeed ✅
- Non-OPTIONS requests properly secured ✅

## Repository Structure
```
product-is/
├── ISSUE_ANALYSIS.md
├── docs/
│   └── OPTIONS_REQUEST_SECURITY_ISSUE.md
└── modules/integration/tests-integration/tests-backend/
    ├── src/test/java/org/wso2/identity/integration/test/resource/access/control/
    │   └── OptionsRequestSecurityTestCase.java
    └── src/test/resources/artifacts/IS/resource-access-control/
        ├── README.md
        └── options-request-test.toml
```

## Next Steps
1. **Submit issue** to carbon-identity-framework repository
2. **Reference this test case** as reproduction steps
3. **Suggest fix**: Move dynamic config processing after OPTIONS exemption
4. **Update documentation** once fix is available

## Impact
- **Severity**: High (breaks CORS functionality)
- **Scope**: All deployments using dynamic API resource configuration with `http_method="all"`
- **Standards Compliance**: Violates HTTP OPTIONS method specifications
- **User Experience**: Breaks web applications and API tools

This comprehensive solution provides both immediate workarounds for users and a clear path forward for fixing the underlying issue in the framework.