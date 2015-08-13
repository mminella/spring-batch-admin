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
import static org.springframework.restdocs.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

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
public class BatchStepExecutionsApiDocumentation  extends AbstractApiDocumentation {

	@Test
	public void testList() throws Exception {
		JobExecution jobExecution = new JobExecution(2l);
		jobExecution.setLastUpdated(new Date());
		StepExecution execution1 = new StepExecution("step1", jobExecution, 1l);
		execution1.setLastUpdated(new Date());
		StepExecution execution2 = new StepExecution("step2", jobExecution, 2l);
		execution2.setLastUpdated(new Date());

		when(jobService.getStepExecutions(2l)).thenReturn(Arrays.asList(execution1, execution2));

		mockMvc.perform(
				get("/batch/executions/{jobExecutionId}/steps", 2l).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print())
				.andDo(document("step-executions-list",
						pathParameters(parameterWithName("jobExecutionId").description("id of the job execution that is the parent of the steps")),
						responseFields(fieldWithPath("stepExecutionInfoResourceList").description("list of <<step-execution-resource>>"))));
	}

	@Test
	public void testDetails() throws Exception {
		JobExecution jobExecution = new JobExecution(2l, new JobParametersBuilder().addString("param1", "test").addLong("param2", 123l).toJobParameters());
		jobExecution.setLastUpdated(new Date());
		StepExecution execution = new StepExecution("step1", jobExecution, 1l);
		execution.setLastUpdated(new Date());
		execution.getExecutionContext().put("contextTestKey", "someValue");

		when(jobService.getStepExecution(2l, 1l)).thenReturn(execution);

		mockMvc.perform(
				get("/batch/executions/{jobExecutionId}/steps/{stepExecutionId}", 2l, 1l).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(print())
				.andDo(document("step-executions-details",
						pathParameters(parameterWithName("jobExecutionId").description("id of the job execution that is the parent of the step"),
								parameterWithName("stepExecutionId").description("id of the step execution")),
						responseFields(fieldWithPath("stepExecutionInfoResource").description("A <<step-execution-resource>>"))));
	}

	@Test
	public void testProgress() throws Exception {
		JobInstance jobInstance = new JobInstance(1l, "job1");
		JobExecution jobExecution = new JobExecution(jobInstance, 2l, new JobParametersBuilder().addString("param1", "test").addLong("param2", 123l).toJobParameters(), null);
		jobExecution.setLastUpdated(new Date());
		StepExecution execution = new StepExecution("step1", jobExecution, 1l);
		execution.setLastUpdated(new Date());

		when(jobService.getStepExecution(2l, 1l)).thenReturn(execution);
		when(jobService.countStepExecutionsForStep("job", "step1")).thenReturn(1);
		when(jobService.listStepExecutionsForStep("job1", "step1", 0, 1000)).thenReturn(Arrays.asList(new StepExecution("step1", new JobExecution(5l))));

		mockMvc.perform(
				get("/batch/executions/{jobExecutionId}/steps/{stepExecutionId}/progress", 2l, 1l).accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(document("step-execution-progress",
						pathParameters(parameterWithName("jobExecutionId").description("id of the job execution that is the parent of the step"),
								parameterWithName("stepExecutionId").description("id of the step execution")),
						responseFields(fieldWithPath("stepExecutionProgressInfoResource").description("A <<step-execution-history-resource>>"))));

	}
}
