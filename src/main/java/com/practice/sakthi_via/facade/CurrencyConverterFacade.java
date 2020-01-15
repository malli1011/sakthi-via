package com.practice.sakthi_via.facade;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.practice.sakthi_via.model.CurrencyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CurrencyConverterFacade {
    /**
     * Logger Object to log the details.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CurrencyConverterFacade.class);
    /**
     * URL to fetch the countries and their currencies.
     */
    @Value("${via.countries.api.url}")
    private String countriesAndCurrenciesUrl;
    /**
     * URL to fetch the currency rate.
     */
    @Value("${via.currencyrate.api.url}")
    private String currencyRateUrl;
    /**
     * RestTemplate object.
     */
    private RestTemplate restTemplate;

    /**
     * Parameterized constructor to bind rest template object.
     *
     * @param restTemplate rest template object
     */
    public CurrencyConverterFacade(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get Countries and their currencies from
     * https://openexchangerates.org/api/currencies.json.
     *
     * @return Countries and their currencies
     */
    @HystrixCommand(fallbackMethod = "getDefaultCountriesAndCurrencies")
    public Map getCountriesAndCurrencies() {
        LOGGER.debug("countriesAndCurrenciesUrl: {}",
                countriesAndCurrenciesUrl);
        HashMap countries = restTemplate
                .getForObject(countriesAndCurrenciesUrl, HashMap.class);
        LOGGER.debug("Countries List: {}", countries);
        return countries;
    }

    /**
     * Get Currency conversion rate from https://api.exchangeratesapi.io/latest.
     *
     * @param base base currency
     * @return currency rates for the base currency
     */
    @HystrixCommand(fallbackMethod = "getDefaultCurrencyRate")
    public CurrencyConverter getCurrencyRate(final String base) {
        String url = String.format(
                currencyRateUrl, base);
        LOGGER.debug("Currency Converter API URL: {}", url);
        CurrencyConverter currencyRate = restTemplate
                .getForObject(url, CurrencyConverter.class);
        if (currencyRate != null) {
            currencyRate.getRates().remove(base);
        }
        LOGGER.debug("Currency Rate: {}", currencyRate);
        return currencyRate;
    }

    /**
     * Get Highest and Lowest currency rate countries for the base currency.
     *
     * @param base base currency
     * @return highest and lowest currency rate countries
     */
    public Map<String, Double> getHighestAndLowestCurrencyRate(
            final String base) {
        CurrencyConverter currencyRate = getCurrencyRate(base);
        Map<String, Double> rates = currencyRate.getRates();

        Optional<Map.Entry<String, Double>> max = rates.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue));

        Optional<Map.Entry<String, Double>> min = rates.entrySet().stream()
                .min(Comparator.comparing(Map.Entry::getValue));

        Map<String, Double> highAndLowRates = new HashMap<>();
        max.ifPresent(entry -> highAndLowRates
                .put(entry.getKey(), entry.getValue()));
        min.ifPresent(entry -> highAndLowRates
                .put(entry.getKey(), entry.getValue()));

        return highAndLowRates;
    }

    /**
     * Get Country for Currency code.
     *
     * @param code currency
     * @return country
     */
    public String getCountryForCurrencyCode(final String code) {
        Map countries = getCountriesAndCurrencies();
        return countries.get(code).toString();
    }

    /**
     * Hystrix fallback method to getCountriesAndCurrencies.
     *
     * @return default values of countries and currencies
     */
    private Map getDefaultCountriesAndCurrencies() {
        Map<String, String> defaultValues = new HashMap<>();
        defaultValues.put("INR", "Indian Rupee");
        defaultValues.put("HUF", "Hungarian Forint");
        return defaultValues;
    }

    /**
     * Hystrix fallback method to getCurrencyRate.
     *
     * @param base Base country
     * @return default values of countries and currencies
     */
    private CurrencyConverter getDefaultCurrencyRate(final String base) {
        final Double inr = 0.2357907805;
        final Double idr = 45.60031709;
        final Double gbp = 0.0025654372;

        CurrencyConverter converter = new CurrencyConverter();
        converter.setBase("HUF");
        converter.setDate(LocalDate.now());

        Map<String, Double> rates = new HashMap<>();
        rates.put("GBP", gbp);
        rates.put("IDR", idr);
        rates.put("INR", inr);
        rates.put("HUF", 1.0);

        converter.setRates(rates);

        return converter;
    }

}
