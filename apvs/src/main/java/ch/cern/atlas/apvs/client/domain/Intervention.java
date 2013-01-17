package ch.cern.atlas.apvs.client.domain;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;

public class Intervention implements Serializable {

	private static final long serialVersionUID = 2578285814293336298L;

	@NotNull
	private Integer userId;
	@NotNull
	private Integer deviceId;

	private int id;
	private String fname;
	private String lname;
	private String ptuId;
	private Date startTime;
	private Date endTime;
	private String impactNumber;
	private Double recStatus;
	private String description;

	public Intervention() {
	}
	
	public Intervention(int id, int userId, String fname, String lname, int deviceId, String ptuId, Date startTime,
			Date endTime, String impactNumber, Double recStatus, String description) {
		this(userId, deviceId, startTime, impactNumber, recStatus, description);
		this.id = id;
		this.fname = fname;
		this.lname = lname;
		this.ptuId = ptuId;
		this.endTime = endTime;
	}

	public Intervention(Integer userId, Integer deviceId, Date startTime, String impactNumber, Double recStatus, String description) {
		this.id = 0;
		this.userId = userId;
		this.deviceId = deviceId;
		this.startTime = startTime;
		this.impactNumber = impactNumber;
		this.recStatus = recStatus;
		this.description = description;
	}

	public Integer getDeviceId() {
		return deviceId;
	}

	public Integer getUserId() {
		return userId;
	}
	
	public String getName() {
		return fname+" "+lname;
	}

	public String getPtuId() {
		return ptuId;
	}
	
	public String getImpactNumber() {
		return impactNumber;
	}
	
	public void setImpactNumber(String impactNumber) {
		this.impactNumber = impactNumber;
	}
	
	public Double getRecStatus() {
		return recStatus;
	}
	
	public void setRecStatus(Double recStatus) {
		this.recStatus = recStatus;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof Intervention) {
			return getId() == ((Intervention)obj).getId();
		}
		return super.equals(obj);
	}
}
