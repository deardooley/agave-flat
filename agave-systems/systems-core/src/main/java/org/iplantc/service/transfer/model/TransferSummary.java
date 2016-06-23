package org.iplantc.service.transfer.model;

import java.math.BigInteger;

public class TransferSummary {

	private long totalTransfers = 0;
	private long totalActiveTransfers = 0;
	private double averageTransferRate = 0;
	private BigInteger totalTransferredBytes = BigInteger.ZERO;
	private BigInteger totalBytes = BigInteger.ZERO;
	
	/**
	 * @return the totalTransfers
	 */
	public long getTotalTransfers()
	{
		return totalTransfers;
	}
	/**
	 * @param totalTransfers the totalTransfers to set
	 */
	public void setTotalTransfers(long totalTransfers)
	{
		this.totalTransfers = totalTransfers;
	}
	/**
	 * @return the totalActiveTransfers
	 */
	public long getTotalActiveTransfers()
	{
		return totalActiveTransfers;
	}
	/**
	 * @param totalActiveTransfers the totalActiveTransfers to set
	 */
	public void setTotalActiveTransfers(long totalActiveTransfers)
	{
		this.totalActiveTransfers = totalActiveTransfers;
	}
	/**
	 * @return the averageTransferRate
	 */
	public double getAverageTransferRate()
	{
		return averageTransferRate;
	}
	/**
	 * @param averageTransferRate the averageTransferRate to set
	 */
	public void setAverageTransferRate(double averageTransferRate)
	{
		this.averageTransferRate = averageTransferRate;
	}
	/**
	 * @return the totalTransferredBytes
	 */
	public BigInteger getTotalTransferredBytes()
	{
		return totalTransferredBytes;
	}
	/**
	 * @param totalTransferredBytes the totalTransferredBytes to set
	 */
	public void setTotalTransferredBytes(BigInteger totalTransferredBytes)
	{
		this.totalTransferredBytes = totalTransferredBytes;
	}
	/**
	 * @return the totalBytes
	 */
	public BigInteger getTotalBytes()
	{
		return totalBytes;
	}
	/**
	 * @param totalBytes the totalBytes to set
	 */
	public void setTotalBytes(BigInteger totalBytes)
	{
		this.totalBytes = totalBytes;
	}
	

}
