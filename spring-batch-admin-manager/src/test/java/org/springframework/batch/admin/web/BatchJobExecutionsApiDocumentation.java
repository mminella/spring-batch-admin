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
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.JobSupport;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BatchJobExecutionsApiDocumentation extends AbstractApiDocumentation {

	@Autowired
	private JobLocator jobLocator;

	private JobExecution execution1;
	private JobExecution execution2;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		JobInstance jobInstance1 = new JobInstance(2l, "job1");
		JobInstance jobInstance2 = new JobInstance(0l, "job1");

		execution1 = new JobExecution(jobInstance1, 3l, new JobParametersBuilder().addString("param1", "test").addLong("param2", 123l, false).toJobParameters(), null);
		execution1.setLastUpdated(new Date());
		execution2 = new JobExecution(jobInstance2, 0l, new JobParametersBuilder().addString("param1", "test").addLong("param2", 123l, false).toJobParameters(), null);
		execution2.setLastUpdated(new Date());

		StepExecution step1 = new StepExecution("step1", execution2);
		step1.setLastUpdated(new Date());
		step1.setId(1l);
		StepExecution step2 = new StepExecution("step2", execution2);
		step2.setLastUpdated(new Date());
		step2.setId(4l);

		execution2.addStepExecutions(Arrays.asList(step1, step2));
	}

	@Test
	public void testList() throws Exception {
		when(jobService.listJobExecutions(5, 5)).thenReturn(Arrays.asList(execution2, execution1));
		when(jobLocator.getJob("job1")).thenReturn(new JobSupport("job1"));
		when(jobService.countJobExecutions()).thenReturn(1);

		mockMvc.perform(
				get("/batch/executions").param("page", "1").param("size", "5").accept(
						MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("job-executions-list",
				queryParameters(parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page")),
				responseFields(fieldWithPath("pagedResources.page").description("<<overview-pagination-response>>"),
						fieldWithPath("pagedResources.content").description("Array of <<job-execution-resource>>"),
						fieldWithPath("pagedResources.links").description("Links to the current page of <<job-execution-resource>>"))));
	}

	@Test
	public void testGetJobExecutionsByName() throws Exception {
		when(jobService.listJobExecutionsForJob("job1", 0, 20)).thenReturn(Collections.singletonList(execution1));
		when(jobLocator.getJob("job1")).thenReturn(new JobSupport("job1"));
		when(jobService.countJobExecutions()).thenReturn(1);

		mockMvc.perform(
				get("/batch/executions").param("jobname", "job1").param("page", "0").param("size", "20").accept(
						MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("job-executions-by-name",
				queryParameters(parameterWithName("jobname").description("the name of the job"),
						parameterWithName("page").description("Requested page index (0 based)"),
						parameterWithName("size").description("Number of elements per page")),
				responseFields(fieldWithPath("pagedResources.page").description("<<overview-pagination-response>>"),
						fieldWithPath("pagedResources.content").description("Array of <<job-execution-resource>>"),
						fieldWithPath("pagedResources.links").description("Links to the current page of <<job-execution-resource>>"))));
	}

	@Test
	public void testGetExecutionsByInstanceId() throws Exception {
		when(jobService.getJobExecutionsForJobInstance("job1", 5l)).thenReturn(Collections.singletonList(execution1));

		mockMvc.perform(
				get("/batch/executions").param("jobinstanceid", "5").param("jobname", "job1")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("job-executions-by-instance-id",
				queryParameters(parameterWithName("jobinstanceid").description("id of the job instance"),
						parameterWithName("jobname").description("name of the job")),
				responseFields(fieldWithPath("jobExecutionInfoResourceList").description("List of <<job-execution-resource>>"))));
	}

	@Ignore("request attributes are not supported...need to re-evaluate if shoudl be a 'JobLaunchRequest'")
	@Test
	public void testLaunchAJob() throws Exception {
		mockMvc.perform(
				post("/batch/executions").requestAttr("jobname", "job1").requestAttr("jobparameters", "foo=1,bar=baz").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andDo(document("launch-job",
			requestFields(fieldWithPath("jobname").description("name of the job to launch.  JSR-352 based jobs would be the name of the xml file"),
				fieldWithPath("jobparameters").description("comma delimited list of parameters")))).andExpect(status().isCreated());
	}

	@Ignore("path parameters are not supported yet.  See https://github.com/spring-projects/spring-restdocs/issues/86")
	@Test
	public void testGetJobExecutionInfo() throws Exception {
		when(jobService.getJobExecution(0l)).thenReturn(execution2);
		when(jobLocator.getJob("job1")).thenReturn(new JobSupport("job1"));

		mockMvc.perform(
				get("/batch/executions/0").accept(MediaType.APPLICATION_JSON)).andDo(print()).andDo(document("get-job-execution"));

	}

	@Ignore("path parameters are not supported yet.  See https://github.com/spring-projects/spring-restdocs/issues/86")
	@Test
	public void testStopJobExecution() throws Exception {
		mockMvc.perform(put("/batch/executions/{executionId}?stop=true", "0")).andDo(print()).andDo(document("stop-job-execution"));
	}

	@Ignore("path parameters are not supported yet.  See https://github.com/spring-projects/spring-restdocs/issues/86")
	@Test
	public void testRestartJob() throws Exception {
		when(jobService.getJobExecution(5l)).thenThrow(new NoSuchJobExecutionException("Could not find jobExecution with id 99999"));

		mockMvc.perform(get("/batch/executions/5").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andDo(document("restart-job"));
	}

	@Test
	public void testStopAllJobs() throws Exception {
		mockMvc.perform(put("/batch/executions?stop=true")).andDo(print()).andDo(document("stop-all-jobs",
		queryParameters(parameterWithName("stop").description("must equal true")))).andExpect(status().isOk());
	}
}
