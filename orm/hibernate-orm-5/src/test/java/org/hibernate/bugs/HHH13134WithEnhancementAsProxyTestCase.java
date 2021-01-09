/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * With enhancement as proxy, it's better (closed to what Hibernate does without lazy initialization)
 */
public class HHH13134WithEnhancementAsProxyTestCase extends BaseCoreFunctionalTestCase {

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                MessageWithLazyToOne.class,
                MessageWithoutLazyToOne.class,
                Patient.class,
                Practitioner.class,
                User.class,
        };
    }

    @Override
    protected String getBaseForMappings() {
        return "org/hibernate/test/";
    }

    // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);

        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());

        // enable enhancement as proxy
        configuration.setProperty(AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, Boolean.TRUE.toString());
    }

    @Before
    public void setup() {

        Session s = openSession();
        Transaction tx = s.beginTransaction();

        for (long i = 0; i < 5; i++) {

            User user = new User()
                    .setLogin("login" + i);
            s.persist(user);

            Practitioner practitioner = new Practitioner()
                    .setId(i)
                    .setUser(user);
            s.persist(practitioner);

            Patient p = new Patient()
                    .setId(i)
                    .addPractitioner(practitioner);
            s.persist(p);

            MessageWithoutLazyToOne mwo = new MessageWithoutLazyToOne()
                    .setId(i)
                    .setPatient(p)
                    .setPractitioner(practitioner);
            s.persist(mwo);

            MessageWithLazyToOne mw = new MessageWithLazyToOne()
                    .setId(i)
                    .setPatient(p)
                    .setPractitioner(practitioner);
            s.persist(mw);

        }

        tx.commit();
        s.close();
    }

    @Test
    public void hhh13134_with_beap_without_LazyToOne() {

        Session s = openSession();
        Transaction tx = s.beginTransaction();

        log.info("Find MessageWithoutLazyToOne...");
        MessageWithoutLazyToOne m = s.find(MessageWithoutLazyToOne.class, 1L);
        // The Message is selected join columns, that's nice Hibernate
        //    select
        //        messagewit0_.id as id1_1_0_,
        //        messagewit0_.patient_id as patient_2_1_0_,
        //        messagewit0_.practitioner_id as practiti3_1_0_
        //    from
        //        MessageWithoutLazyToOne messagewit0_
        //    where
        //        messagewit0_.id=?
        log.info("Getting Message.patient.id...");
        m.getPatient().getId();
        // no more query as we know the id!
        log.info("Getting Message.practitioner.user.login...");
        m.getPractitioner().getUser().getLogin();
        // loading the associated practioner and its join column
        // select
        //        practition0_.id as id1_4_0_,
        //        practition0_.user_login as user_log2_4_0_
        //    from
        //        Practitioner practition0_
        //    where
        //        practition0_.id=?

        tx.commit();
        s.close();
    }

    @Test
    public void hhh13134_with_beap_without_LazyToOne_join_fetch() {

        Session s = openSession();
        Transaction tx = s.beginTransaction();

        log.info("selecting a MessageWithoutLazyToOne with join fetch 2 associations...");
        MessageWithoutLazyToOne m = s.createQuery(
                "SELECT m " +
                        "FROM MessageWithoutLazyToOne m " +
                        "JOIN FETCH m.patient " +
                        "JOIN FETCH m.practitioner " +
                        "WHERE m.id = 1L",
                MessageWithoutLazyToOne.class).getSingleResult();
        // Wonderful, everything is here in 1 query!
        //    select
        //        messagewit0_.id as id1_1_0_,
        //        patient1_.id as id1_2_1_,
        //        practition2_.id as id1_4_2_,
        //        messagewit0_.patient_id as patient_2_1_0_,
        //        messagewit0_.practitioner_id as practiti3_1_0_,
        //        practition2_.user_login as user_log2_4_2_
        //    from
        //        MessageWithoutLazyToOne messagewit0_
        //    inner join
        //        Patient patient1_
        //            on messagewit0_.patient_id=patient1_.id
        //    inner join
        //        Practitioner practition2_
        //            on messagewit0_.practitioner_id=practition2_.id
        //    where
        //        messagewit0_.id=1
        log.info("Getting Message.patient.id...");
        m.getPatient().getId();
        // no more query
        log.info("Getting Message.practitioner.user.login...");
        m.getPractitioner().getUser().getLogin();
        // no more query
        tx.commit();
        s.close();
    }
}
