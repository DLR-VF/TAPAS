package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum PersonGroup {
	PG_1(0, generateSet(Job.JOB_5, Job.JOB_11, Job.JOB_19)), //
	PG_2(1, generateSet(Job.JOB_12, Job.JOB_20)), //
	PG_3(2, generateSet(Job.JOB_3, Job.JOB_9, Job.JOB_17)), //
	PG_4(3, generateSet(Job.JOB_10, Job.JOB_18)), //
	PG_5(4, generateSet(Job.JOB_8, Job.JOB_15, Job.JOB_23)), //
	PG_6(5, generateSet(Job.JOB_16, Job.JOB_24)), //
	PG_7(6, generateSet(Job.JOB_6, Job.JOB_13, Job.JOB_21)), //
	PG_8(7, generateSet(Job.JOB_14, Job.JOB_22)), //
	PG_9(8, generateSet(Job.JOB_25, Job.JOB_27, Job.JOB_29, Job.JOB_31)), //
	PG_10(9, generateSet(Job.JOB_26, Job.JOB_28, Job.JOB_30, Job.JOB_32)), //
	PG_11(10, generateSet(Job.JOB_1, Job.JOB_2)), //
	PG_12(11, generateSet()), //
	PG_13(12, generateSet(Job.JOB_4, Job.JOB_7));

	private final int id;
	private final Set<Job> jobSet;

	PersonGroup(int id, Set<Job> jobSet) {
		this.id = id;
		this.jobSet = jobSet;
	}

	public Set<Job> getJobSet() {
		return jobSet;
	}

	public int getId() {
		return id;
	}
	
	public String getName(){
		return toString();
	}

	/**
	 * 
	 * @param id
	 * @return den Wegezweck der dieser ID zugeordnet ist
	 * @throws IllegalArgumentException
	 *             geworfen wenn die ID keinem Wegezweck zugeordnet werden
	 *             konnte
	 */
	public static PersonGroup getById(int id) throws IllegalArgumentException {
		for (PersonGroup pg : PersonGroup.values()) {
			if (pg.getId() == id) {
				return pg;
			}
		}

		throw new IllegalArgumentException();
	}

	public static PersonGroup getByJob(Job job) {
		for (PersonGroup group : PersonGroup.values()) {
			if (group.getJobSet().contains(job)) {
				return group;
			}
		}

		throw new IllegalArgumentException(
				"Job is not assigned to any personGroup");

	}

	private static Set<Job> generateSet(Job... jobs) {
		return new HashSet<>(Arrays.asList(jobs));
	}
}