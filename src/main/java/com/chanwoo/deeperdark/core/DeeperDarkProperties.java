package com.chanwoo.deeperdark.core;

import com.chanwoo.deeperdark.common.properties.TripleBlockPart;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;

public final class DeeperDarkProperties {
    public static final EnumProperty<TripleBlockPart> TRIPLE_BLOCK_HALF = EnumProperty.of("third", TripleBlockPart.class);
    public static final BooleanProperty RINGING = BooleanProperty.of("ringing");
    public static final BooleanProperty BITING = BooleanProperty.of("biting");

    private DeeperDarkProperties() {
    }
}
