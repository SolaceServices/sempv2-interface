/**
 * 
 */
package com.solace.psg.sempv2.common;

import com.solace.psg.sempv2.config.model.SempMeta;

/**
 * Abstract Class for Semp Reponse types with common parameters.
 * @author VictorTsonkov
 *
 */
public abstract class SempResponse
{
    public abstract SempMeta getMeta();
}
