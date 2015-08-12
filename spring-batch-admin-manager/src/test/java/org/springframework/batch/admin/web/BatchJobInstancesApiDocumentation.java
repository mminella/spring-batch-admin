/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.batch.admin.web;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.RestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDependencies.class, RestConfiguration.class})
@WebAppConfiguration
public class BatchJobInstancesApiDocumentation extends AbstractApiDocumentation {

	private JobExecution execution;

	private JobInstance jobInstance;

	@Before
	public void before() throws Exception {
		jobInstance = new JobInstance(0l, "job1");

		Date startTime = new Date();
		Date endTime = new Date();
		execution = new JobExecution(3L,
				new JobParametersBuilder().addString("foo", "bar").addLong("foo2", 0L).toJobParameters());
		execution.setExitStatus(ExitStatus.COMPLETED);
		execution.setStartTime(startTime);
		execution.setEndTime(endTime);
		execution.setLastUpdated(new Date());

		StepExecution stepExecution = new StepExecution("s1", execution);
		stepExecution.setLastUpdated(new Date());
		stepExecution.setId(1l);
		execution.addStepExecutions(Collections.singletonList(stepExecution));
	}

	@Ignore("path parameters are not supported yet.  See https://github.com/spring-projects/spring-restdocs/issues/86")
	@Test
	public void testGetJobInstance() throws Exception {
		mockMvc.perform(
				get("/batch/instances/0").accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("get-job-instance"));

	}

	@Test
	public void testGetInstancesForJob() throws Exception {
		when(jobService.listJobInstances("job1", 0, 20)).thenReturn(Arrays.asList(jobInstance, new JobInstance(3l, "job1")));
		when(jobService.getJobExecutionsForJobInstance(jobInstance.getJobName(), jobInstance.getId())).thenReturn(Collections.singletonList(execution));

		mockMvc.perform(
				get("/batch/instances")
						.param("jobname", "job1")
						.param("page", "0")
						.param("size", "20")
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andDo(document("instances-for-job",
				queryParameters(parameterWithName("jobname").description("name of the job"),
						parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page")),
				responseFields(fieldWithPath("pagedResources.page").description("<<overview-pagination-response>>"),
					fieldWithPath("pagedResources.content").description("Array of <<job-instance-resource>>"),
					fieldWithPath("pagedResources.links").description("Links to the current page of <<job-instance-resource>>"))));
	}
}