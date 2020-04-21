package de.dlr.ivf.tapas.analyzer.inputfileconverter;

public enum Job {
	JOB_1(1), JOB_2(2), JOB_3(3), JOB_4(4), JOB_5(5), JOB_6(6), JOB_7(7), JOB_8(8), JOB_9(9), JOB_10(10), JOB_11(11), JOB_12(
			12), JOB_13(13), JOB_14(14), JOB_15(15), JOB_16(16), JOB_17(17), JOB_18(18), JOB_19(19), JOB_20(20), JOB_21(
			21), JOB_22(22), JOB_23(23), JOB_24(24), JOB_25(25), JOB_26(26), JOB_27(27), JOB_28(28), JOB_29(29), JOB_30(
			30), JOB_31(31), JOB_32(32);

	private final int id;

	Job(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static Job getById(int job) {
		for (Job j : Job.values()) {
			if (j.getId() == job) {
				return j;
			}
		}
		throw new IllegalArgumentException();
	}
}
