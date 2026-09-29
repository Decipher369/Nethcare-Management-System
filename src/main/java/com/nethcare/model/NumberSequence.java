package com.nethcare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "number_sequences")
public class NumberSequence {
    @Id
    @Column(name = "sequence_name", length = 40)
    private String sequenceName;
    @Column(name = "next_value", nullable = false)
    private long nextValue;

    public String getSequenceName() { return sequenceName; }
    public void setSequenceName(String sequenceName) { this.sequenceName = sequenceName; }
    public long getNextValue() { return nextValue; }
    public void setNextValue(long nextValue) { this.nextValue = nextValue; }
}
