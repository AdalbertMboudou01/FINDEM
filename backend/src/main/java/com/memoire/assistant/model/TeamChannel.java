package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "team_channels")
public class TeamChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID channelId;

    private String name;

    @Column(name = "channel_type")
    private String type; // OFFER | GENERAL | WORKSPACE

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "recruiter_id")
    private UUID recruiterId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "created_at")
    private Date createdAt;

    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public UUID getRecruiterId() { return recruiterId; }
    public void setRecruiterId(UUID recruiterId) { this.recruiterId = recruiterId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
