/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.integration.test.resource.access.control;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.identity.integration.common.utils.ISIntegrationTest;
import org.wso2.identity.integration.test.utils.ServerConfigurationManager;
import org.wso2.identity.integration.common.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Test case to verify OPTIONS requests are not treated as secured
 * even when dynamic API resource access control is configured.
 */
public class OptionsRequestSecurityTestCase extends ISIntegrationTest {

    private ServerConfigurationManager serverConfigurationManager;
    private CloseableHttpClient httpClient;

    @BeforeClass(alwaysRun = true)
    public void testInit() throws Exception {

        super.init(TestUserMode.SUPER_TENANT_USER);
        
        // Apply deployment.toml with dynamic API resource configuration
        String carbonHome = Utils.getResidentCarbonHome();
        File defaultTomlFile = getDeploymentTomlFile(carbonHome);
        File testConfigFile = new File(getISResourceLocation() + File.separator + "resource-access-control" 
                + File.separator + "options-request-test.toml");

        serverConfigurationManager = new ServerConfigurationManager(isServer);
        serverConfigurationManager.applyConfigurationWithoutRestart(testConfigFile, defaultTomlFile, true);
        serverConfigurationManager.restartGracefully();

        super.init(TestUserMode.SUPER_TENANT_USER);
        httpClient = HttpClients.createDefault();
    }

    @AfterClass(alwaysRun = true)
    public void atEnd() throws Exception {

        if (httpClient != null) {
            httpClient.close();
        }
        
        if (serverConfigurationManager != null) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    @Test(groups = "wso2.is", description = "Test OPTIONS request to dynamically secured SCIM2 Agents endpoint")
    public void testOptionsRequestToScim2AgentsEndpoint() throws IOException {

        String scim2AgentsEndpoint = getServerURL() + "/scim2/Agents";
        
        // Test OPTIONS request - should be allowed (not secured)
        HttpOptions optionsRequest = new HttpOptions(scim2AgentsEndpoint);
        HttpResponse optionsResponse = httpClient.execute(optionsRequest);
        
        // OPTIONS should return 200 or 204, not 401 Unauthorized
        int optionsStatusCode = optionsResponse.getStatusLine().getStatusCode();
        Assert.assertTrue(optionsStatusCode == 200 || optionsStatusCode == 204 || optionsStatusCode == 405,
                "OPTIONS request should not be secured. Expected status: 200/204/405, but got: " + optionsStatusCode);
        
        // Test GET request - should be secured (require authentication)
        HttpGet getRequest = new HttpGet(scim2AgentsEndpoint);
        HttpResponse getResponse = httpClient.execute(getRequest);
        
        // GET should return 401 Unauthorized (secured)
        int getStatusCode = getResponse.getStatusLine().getStatusCode();
        Assert.assertEquals(getStatusCode, 401, 
                "GET request should be secured and return 401 Unauthorized, but got: " + getStatusCode);
    }

    @Test(groups = "wso2.is", description = "Test OPTIONS request to dynamically secured API resource endpoint")
    public void testOptionsRequestToApiResourceEndpoint() throws IOException {

        String apiResourceEndpoint = getServerURL() + "/api/server/v1/api-resources";
        
        // Test OPTIONS request - should be allowed (not secured)
        HttpOptions optionsRequest = new HttpOptions(apiResourceEndpoint);
        HttpResponse optionsResponse = httpClient.execute(optionsRequest);
        
        // OPTIONS should return 200 or 204, not 401 Unauthorized
        int optionsStatusCode = optionsResponse.getStatusLine().getStatusCode();
        Assert.assertTrue(optionsStatusCode == 200 || optionsStatusCode == 204 || optionsStatusCode == 405,
                "OPTIONS request should not be secured. Expected status: 200/204/405, but got: " + optionsStatusCode);
    }

    @Test(groups = "wso2.is", description = "Test CORS preflight OPTIONS request")
    public void testCorsPreflightOptionsRequest() throws IOException {

        String testEndpoint = getServerURL() + "/scim2/Agents";
        
        // Create CORS preflight OPTIONS request
        HttpOptions optionsRequest = new HttpOptions(testEndpoint);
        optionsRequest.setHeader("Origin", "https://example.com");
        optionsRequest.setHeader("Access-Control-Request-Method", "GET");
        optionsRequest.setHeader("Access-Control-Request-Headers", "Authorization, Content-Type");
        
        HttpResponse optionsResponse = httpClient.execute(optionsRequest);
        
        // CORS preflight should not be blocked by authentication
        int statusCode = optionsResponse.getStatusLine().getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 204 || statusCode == 405,
                "CORS preflight OPTIONS request should not be secured. Expected status: 200/204/405, but got: " + statusCode);
    }

    private String getServerURL() {
        return "https://localhost:9853";
    }
}