/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.content;

/**
 *
 * @author xvas
 */
public interface IContent {

    @Override
    public boolean equals(Object obj);

    public long getID();

    @Override
    public int hashCode();

    public long sizeInBytes();

    public double sizeInMBs();

    public double sizeInChunks();

    @Override
    public String toString();

    public ContentDocument referredContentDocument();

    public double costOfTransferSCCacheHit() throws Throwable;

    public double costOfTransferSC_BH() throws Throwable;

    public double costOfTransferMC_BH() throws Throwable;

    public double gainOfTransferSCCacheHit() throws Throwable;

    public double gainOfTransferSCThroughBH() throws Throwable;

}
