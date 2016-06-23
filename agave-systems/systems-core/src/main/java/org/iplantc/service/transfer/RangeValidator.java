/**
 * 
 */
package org.iplantc.service.transfer;

import org.iplantc.service.transfer.exceptions.RangeValidationException;
import org.iplantc.service.transfer.model.Range;

/**
 * @author dooley
 *
 */
public class RangeValidator {

    private long index;
    private long size;
    private long length;
    
    /**
     * Define a range over which to validate
     * 
     * @param index start of range
     * @param size size of range
     * @param length total length of resource on which the range applies
     */
    public RangeValidator(long index, long size, long length) {
        this.index = index;
        this.size = size;
        this.length = length;
    }
    
    /**
     * Define a range over which to validate
     * 
     * @param index start of range
     * @param size size of range
     * @param length total length of resource on which the range applies
     */
    public RangeValidator(Range range, long length) {
        this(range.getIndex(), range.getSize(), length);
    }

    /**
     * Validates that the given {@link Range} defined by {@link #getIndex()} and
     * {@link #getSize()} fit within the bounds of {@link #getLength()}, adjusting
     * for negative values.
     * 
     * @throws RangeValidationException
     */
    public void validate() throws RangeValidationException  {
        Range fullRange = new Range(0, length);
        
        if (!fullRange.isIncluded(index, length)) {
            throw new RangeValidationException("Range offset cannot be greater than the total length");
        } else if (size == 0) {
            throw new RangeValidationException("Range size must be greater than zero");
        } else {
            fullRange.setIndex(index);
            
            if (!fullRange.isIncluded(index + size, length)) {
                throw new RangeValidationException("Range size cannot exceed end of file");
            }
        }
    }
    
    /**
     * Calculates the absolute index of a range index from position 0, adjusting for negative
     * values after calling the {@link #validate()} method. If the {link #getLength()} is 
     * {@link Range#SIZE_MAX}, {@link Range#INDEX_LAST}.
     * 
     * @return the index from position 0 or {@link Range#INDEX_LAST} if {link #getLength()} is negative
     * and {link #getIndex()} is less than zero.
     * @throws RangeValidationException
     */
    public long getAbsoluteIndex() throws RangeValidationException 
    {   
        // validate the index so we do not return false positives 
        validate();
        
        if (index < 0) {
            if (index == Range.INDEX_LAST) {
                // The range starts from the end
                return length == Range.SIZE_MAX ? Range.INDEX_LAST : length - 1;
            } else {
                return length - (index) - 1;
            }
        } else {
            return index;
        }
    }

}
