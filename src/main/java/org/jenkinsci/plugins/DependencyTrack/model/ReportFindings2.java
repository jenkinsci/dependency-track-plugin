package org.jenkinsci.plugins.DependencyTrack.model;

public final class ReportFindings2 {
	private final String componentName;
	private final String componentGroup;
	private final String componentVersion;
	private final String vulnerabilityID;
	private final Integer severity;
	private final String cwe;
	private final boolean isSuppressed;

	ReportFindings2(final String componentName, final String componentGroup, final String componentVersion, final String vulnerabilityID, final Integer severity, final String cwe, final boolean isSuppressed) {
		this.componentName = componentName;
		this.componentGroup = componentGroup;
		this.componentVersion = componentVersion;
		this.vulnerabilityID = vulnerabilityID;
		this.severity = severity;
		this.cwe = cwe;
		this.isSuppressed = isSuppressed;
	}


	public static class ReportFindingsBuilder {
		private String componentName;
		private String componentGroup;
		private String componentVersion;
		private String vulnerabilityID;
		private Integer severity;
		private String cwe;
		private boolean isSuppressed;

		ReportFindingsBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder componentName(final String componentName) {
			this.componentName = componentName;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder componentGroup(final String componentGroup) {
			this.componentGroup = componentGroup;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder componentVersion(final String componentVersion) {
			this.componentVersion = componentVersion;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder vulnerabilityID(final String vulnerabilityID) {
			this.vulnerabilityID = vulnerabilityID;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder severity(final Integer severity) {
			this.severity = severity;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder cwe(final String cwe) {
			this.cwe = cwe;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		public ReportFindings2.ReportFindingsBuilder isSuppressed(final boolean isSuppressed) {
			this.isSuppressed = isSuppressed;
			return this;
		}

		public ReportFindings2 build() {
			return new ReportFindings2(this.componentName, this.componentGroup, this.componentVersion, this.vulnerabilityID, this.severity, this.cwe, this.isSuppressed);
		}

		@Override
		public String toString() {
			return "ReportFindings.ReportFindingsBuilder(componentName=" + this.componentName + ", componentGroup=" + this.componentGroup + ", componentVersion=" + this.componentVersion + ", vulnerabilityID=" + this.vulnerabilityID + ", severity=" + this.severity + ", cwe=" + this.cwe + ", isSuppressed=" + this.isSuppressed + ")";
		}
	}

	public static ReportFindings2.ReportFindingsBuilder builder() {
		return new ReportFindings2.ReportFindingsBuilder();
	}

	public String getComponentName() {
		return this.componentName;
	}

	public String getComponentGroup() {
		return this.componentGroup;
	}

	public String getComponentVersion() {
		return this.componentVersion;
	}

	public String getVulnerabilityID() {
		return this.vulnerabilityID;
	}

	public Integer getSeverity() {
		return this.severity;
	}

	public String getCwe() {
		return this.cwe;
	}

	public boolean isSuppressed() {
		return this.isSuppressed;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof ReportFindings2)) return false;
		final ReportFindings2 other = (ReportFindings2) o;
		if (this.isSuppressed() != other.isSuppressed()) return false;
		final Object this$severity = this.getSeverity();
		final Object other$severity = other.getSeverity();
		if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
		final Object this$componentName = this.getComponentName();
		final Object other$componentName = other.getComponentName();
		if (this$componentName == null ? other$componentName != null : !this$componentName.equals(other$componentName)) return false;
		final Object this$componentGroup = this.getComponentGroup();
		final Object other$componentGroup = other.getComponentGroup();
		if (this$componentGroup == null ? other$componentGroup != null : !this$componentGroup.equals(other$componentGroup)) return false;
		final Object this$componentVersion = this.getComponentVersion();
		final Object other$componentVersion = other.getComponentVersion();
		if (this$componentVersion == null ? other$componentVersion != null : !this$componentVersion.equals(other$componentVersion)) return false;
		final Object this$vulnerabilityID = this.getVulnerabilityID();
		final Object other$vulnerabilityID = other.getVulnerabilityID();
		if (this$vulnerabilityID == null ? other$vulnerabilityID != null : !this$vulnerabilityID.equals(other$vulnerabilityID)) return false;
		final Object this$cwe = this.getCwe();
		final Object other$cwe = other.getCwe();
		if (this$cwe == null ? other$cwe != null : !this$cwe.equals(other$cwe)) return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + (this.isSuppressed() ? 79 : 97);
		final Object $severity = this.getSeverity();
		result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
		final Object $componentName = this.getComponentName();
		result = result * PRIME + ($componentName == null ? 43 : $componentName.hashCode());
		final Object $componentGroup = this.getComponentGroup();
		result = result * PRIME + ($componentGroup == null ? 43 : $componentGroup.hashCode());
		final Object $componentVersion = this.getComponentVersion();
		result = result * PRIME + ($componentVersion == null ? 43 : $componentVersion.hashCode());
		final Object $vulnerabilityID = this.getVulnerabilityID();
		result = result * PRIME + ($vulnerabilityID == null ? 43 : $vulnerabilityID.hashCode());
		final Object $cwe = this.getCwe();
		result = result * PRIME + ($cwe == null ? 43 : $cwe.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "ReportFindings(componentName=" + this.getComponentName() + ", componentGroup=" + this.getComponentGroup() + ", componentVersion=" + this.getComponentVersion() + ", vulnerabilityID=" + this.getVulnerabilityID() + ", severity=" + this.getSeverity() + ", cwe=" + this.getCwe() + ", isSuppressed=" + this.isSuppressed() + ")";
	}
}
