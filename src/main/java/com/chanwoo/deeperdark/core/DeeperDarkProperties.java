package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.properties.TripleBlockPart;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;

public final class DeeperDarkProperties {
    public static final EnumProperty<TripleBlockPart> TRIPLE_BLOCK_HALF = EnumProperty.of("third", TripleBlockPart.class);
    public static final BooleanProperty RINGING = BooleanProperty.of("ringing");
    public static final BooleanProperty BITING = BooleanProperty.of("biting");
    /** How full a sculk capacitor looks, from empty (0) to full (4). */
    public static final IntProperty CHARGE_LEVEL = IntProperty.of("charge", 0, 4);

    private DeeperDarkProperties() {
    }
}
