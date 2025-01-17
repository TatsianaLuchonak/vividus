/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.selenium;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public enum WebDriverType
{
    FIREFOX(true, true, true, Set.of(FirefoxOptions.FIREFOX_OPTIONS), BrowserType.FIREFOX, BrowserType.FIREFOX_PROXY)
    {
        @Override
        public void prepareCapabilities(DesiredCapabilities desiredCapabilities)
        {
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            firefoxOptions.addPreference("startup.homepage_welcome_url.additional", "about:blank");
            desiredCapabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, firefoxOptions);
        }

        @Override
        public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities, WebDriverConfiguration configuration)
        {
            prepareCapabilities(desiredCapabilities);
            configuration.getBinaryPath()
                    .ifPresent(binaryPath -> desiredCapabilities.setCapability(FirefoxDriver.BINARY, binaryPath));
            FirefoxOptions firefoxOptions = new FirefoxOptions(desiredCapabilities);
            configuration.getCommandLineArguments().ifPresent(firefoxOptions::addArguments);
            return new FirefoxDriver(firefoxOptions);
        }

        @Override
        void setDriverExecutablePath(Optional<String> driverExecutablePath)
        {
            setDriverExecutablePathImpl(driverExecutablePath, GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY,
                    WebDriverManager::firefoxdriver);
        }
    },
    IEXPLORE(false, true, true, Set.of(WebDriverType.IE_OPTIONS), BrowserType.IEXPLORE, BrowserType.IE)
    {
        @Override
        public void prepareCapabilities(DesiredCapabilities desiredCapabilities)
        {
            // Workaround for IExplore:
            // https://stackoverflow.com/questions/50287435/actions-movetoelement-not-working-on-ie-11
            desiredCapabilities.merge(new InternetExplorerOptions().requireWindowFocus());
        }

        @Override
        public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities, WebDriverConfiguration configuration)
        {
            prepareCapabilities(desiredCapabilities);
            configuration.getCommandLineArguments().ifPresent(arguments ->
            {
                desiredCapabilities.setCapability(InternetExplorerDriver.FORCE_CREATE_PROCESS, true);
                desiredCapabilities.setCapability(InternetExplorerDriver.IE_SWITCHES, arguments);
            });
            return new InternetExplorerDriver(null, new InternetExplorerOptions(desiredCapabilities));
        }

        @Override
        void setDriverExecutablePath(Optional<String> driverExecutablePath)
        {
            setDriverExecutablePathImpl(driverExecutablePath, InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY,
                    WebDriverManager::iedriver);
        }
    },
    CHROME(true, true, false, Set.of(ChromeOptions.CAPABILITY), BrowserType.CHROME, BrowserType.GOOGLECHROME)
    {
        @Override
        public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities, WebDriverConfiguration configuration)
        {
            ChromeOptions chromeOptions = new ChromeOptions();
            configuration.getBinaryPath().ifPresent(chromeOptions::setBinary);
            configuration.getCommandLineArguments().ifPresent(chromeOptions::addArguments);
            configuration.getExperimentalOptions().forEach(chromeOptions::setExperimentalOption);
            chromeOptions.merge(desiredCapabilities);
            return new ChromeDriver(chromeOptions);
        }

        @Override
        void setDriverExecutablePath(Optional<String> driverExecutablePath)
        {
            setDriverExecutablePathImpl(driverExecutablePath, ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY,
                    WebDriverManager::chromedriver);
        }
    },
    SAFARI(false, false, false, Set.of("safari:automaticInspection", "safari:automaticProfiling"),
        BrowserType.SAFARI, BrowserType.SAFARI_PROXY)
    {
        @Override
        public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities, WebDriverConfiguration configuration)
        {
            return new SafariDriver(SafariOptions.fromCapabilities(desiredCapabilities));
        }
    },
    EDGE(false, false, false, Set.of(), BrowserType.EDGE)
    {
        @Override
        public WebDriver getWebDriver(DesiredCapabilities desiredCapabilities, WebDriverConfiguration configuration)
        {
            EdgeOptions edgeOptions = new EdgeOptions();
            edgeOptions.merge(desiredCapabilities);
            return new EdgeDriver(edgeOptions);
        }

        @Override
        void setDriverExecutablePath(Optional<String> driverExecutablePath)
        {
            setDriverExecutablePathImpl(driverExecutablePath, EdgeDriverService.EDGE_DRIVER_EXE_PROPERTY,
                    WebDriverManager::edgedriver);
        }
    };

    public static final String IE_OPTIONS = "se:ieOptions";

    private final boolean binaryPathSupported;
    private final boolean commandLineArgumentsSupported;
    private final boolean useW3C;
    private final Set<String> driverSpecificCapabilities;
    private final String[] browserNames;

    WebDriverType(boolean binaryPathSupported, boolean commandLineArgumentsSupported, boolean useW3C,
            Set<String> driverSpecificCapabilities, String... browserNames)
    {
        this.binaryPathSupported = binaryPathSupported;
        this.commandLineArgumentsSupported = commandLineArgumentsSupported;
        this.useW3C = useW3C;
        this.driverSpecificCapabilities = driverSpecificCapabilities;
        this.browserNames = browserNames;
    }

    public void prepareCapabilities(@SuppressWarnings("unused") DesiredCapabilities desiredCapabilities)
    {
        // Nothing to do by default
    }

    public abstract WebDriver getWebDriver(DesiredCapabilities desiredCapabilities,
            WebDriverConfiguration configuration);

    void setDriverExecutablePath(Optional<String> driverExecutablePath)
    {
        if (driverExecutablePath.isPresent())
        {
            throw new UnsupportedOperationException();
        }
    }

    public boolean isBinaryPathSupported()
    {
        return binaryPathSupported;
    }

    public boolean isCommandLineArgumentsSupported()
    {
        return commandLineArgumentsSupported;
    }

    public boolean isUseW3C()
    {
        return useW3C;
    }

    public Set<String> getDriverSpecificCapabilities()
    {
        return driverSpecificCapabilities;
    }

    public String[] getBrowserNames()
    {
        return ArrayUtils.clone(browserNames);
    }

    private static void setDriverExecutablePathImpl(Optional<String> driverExecutablePath, String driverExePropertyName,
            Supplier<WebDriverManager> webDriverManagerSupplier)
    {
        driverExecutablePath.ifPresentOrElse(path -> System.setProperty(driverExePropertyName, path),
            () -> webDriverManagerSupplier.get().setup());
    }
}
