/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.definition.evaluator.covid;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.covid.ContactsConvertedToCasesDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.evaluator.PersonDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

/**
 * Evaluates a Contacts Converted To Cases Data Definition
 */
@Handler(supports = ContactsConvertedToCasesDataDefinition.class, order = 50)
public class ContactsConvertedToCasesDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry = "select f.case_patient,count(f.patient_id) from\n" +
                "(\n" +
                "select c.patient_id, self_q.patient_id selfQ,c.patient_related_to case_patient, min(self_q.visit_date) first_self_q_followup_date, gov_q.patient_id govQ, min(gov_q.visit_date) first_gov_quarantine_date\n" +
                "from kenyaemr_hiv_testing_patient_contact c\n" +
                "       inner join kenyaemr_etl.etl_covid_19_enrolment e on e.patient_id = c.patient_related_to\n" +
                "       left join kenyaemr_etl.etl_contact_tracing_followup self_q on self_q.patient_id = c.patient_id\n" +
                "       left join kenyaemr_etl.etl_covid_quarantine_enrolment gov_q on gov_q.patient_id = c.patient_id\n" +
                "where c.voided=0 and e.voided = 0\n" +
                ") f where (selfQ is not null and datediff(:endDate,first_self_q_followup_date)>14) or (govQ is not null and datediff(:startDate, first_gov_quarantine_date) > 14)\n" +
                "group by f.case_patient;";

        SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
        Date startDate = (Date) context.getParameterValue("startDate");
        Date endDate = (Date) context.getParameterValue("endDate");
        queryBuilder.addParameter("endDate", endDate);
        queryBuilder.addParameter("startDate", startDate);
        queryBuilder.append(qry);
        Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
        c.setData(data);
        return c;
    }
}