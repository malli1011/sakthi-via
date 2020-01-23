package com.practice.sakthi_via.facade;

import com.practice.sakthi_via.mail.EmailService;
import com.practice.sakthi_via.model.CurrencyConverter;
import com.practice.sakthi_via.model.Employee;
import com.practice.sakthi_via.model.Mail;
import com.practice.sakthi_via.model.RatesRegister;
import com.practice.sakthi_via.repository.RatesRegisterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class SchedulerFacade {
    /**
     * Logger Object to log the details.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SchedulerFacade.class);
    /**
     * Scheduler mail subject.
     */
    private static final String EMAIL_SUBJECT = "<SAKTHI-VIA> Currency Rate "
            + "as of " + LocalDate.now();
    /**
     * Scheduler mail template.
     */
    private static final String MAIL_TEMPLATE = "schedulerMailTemplate";
    /**
     * EmailService object.
     */
    private final EmailService emailService;
    /**
     * RatesRegisterRepository object.
     */
    private final RatesRegisterRepository registerRepository;
    /**
     * CurrencyConverterFacade object.
     */
    private final CurrencyConverterFacade currencyConverterFacade;
    /**
     * CacheManager object.
     */
    private final CacheManager cacheManager;

    /**
     * Parameterized constructor to bind the objects.
     *
     * @param emailService            EmailService object
     * @param registerRepository      RatesRegisterRepository object
     * @param currencyConverterFacade CurrencyConverterFacade object
     * @param cacheManager            CacheManager object
     */
    public SchedulerFacade(final EmailService emailService,
                           final RatesRegisterRepository registerRepository,
                           final CurrencyConverterFacade
                                   currencyConverterFacade,
                           final CacheManager cacheManager) {
        this.emailService = emailService;
        this.registerRepository = registerRepository;
        this.currencyConverterFacade = currencyConverterFacade;
        this.cacheManager = cacheManager;
    }

    /**
     * Method to schedule the currency rate.
     */
    @Scheduled(cron = "${via.scheduler.cron.value}")
    public void dailyEmailAlertScheduler() {

        Map<String, Map<Set<String>, List<RatesRegister>>>
                registeredForAlerts = getAlertRegistrationDetails();

        registeredForAlerts.forEach((baseCode, targetDetailsMap) ->
                targetDetailsMap.forEach((targetsSet, ratesRegistersList) -> {
                    sendMail(baseCode, getLatestRates(baseCode, targetsSet),
                            getToAddresses(ratesRegistersList));
                }));
    }

    private Map<String, Map<Set<String>,
            List<RatesRegister>>> getAlertRegistrationDetails() {
        List<RatesRegister> ratesRegisters = registerRepository.findAll();
        LOGGER.debug("Rates registers: {}", ratesRegisters);

        Map<String, Map<Set<String>, List<RatesRegister>>>
                groupByBaseTargets = ratesRegisters.stream().collect(
                Collectors.groupingBy(RatesRegister::getBase,
                        Collectors.groupingBy(RatesRegister::getTarget)
                ));
        LOGGER.debug("Rates registers Group by: {}", groupByBaseTargets);

        return groupByBaseTargets;
    }

    private Map<String, Double> getLatestRates(
            final String baseCode,
            final Set<String> targetsSet) {
        CurrencyConverter currencyRate = currencyConverterFacade
                .getCurrencyRateWithTarget(baseCode,
                        targetsSet);
        return currencyRate.getRates();
    }

    private StringJoiner getToAddresses(
            final List<RatesRegister> ratesRegistersList) {
        StringJoiner toAddress = new StringJoiner(",");
        ratesRegistersList.forEach(ratesRegister -> {
            Employee employee = ratesRegister.getEmployee();
            LOGGER.debug("Employee: {}", employee);
            toAddress.add(employee.getEmail());
        });
        return toAddress;
    }

    private void sendMail(final String key,
                          final Map<String, Double> targets,
                          final StringJoiner toAddress) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("base", key);
            content.put("targets", targets);

            Mail.MailBuilder builder = Mail.builder();
            builder.setTo(toAddress.toString());
            builder.setSubject(EMAIL_SUBJECT);
            builder.setContent(content);
            builder.setTemplate(MAIL_TEMPLATE);

            Mail mail = builder.createMail();
            emailService.sendMail(mail);
        } catch (MessagingException e) {
            LOGGER.error("Exception in Schedule Mail", e);
        }
    }

    /**
     * To clear all the caches at regular intervals.
     */
    @Scheduled(fixedRateString = "${via.scheduler.cache.evict.value}")
    public void evictAllCachesAtIntervals() {
        LOGGER.debug("Caches are: {}", cacheManager.getCacheNames());
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(
                        cacheManager.getCache(cacheName)).clear());
        LOGGER.debug("Caches cleared!");
    }
}
