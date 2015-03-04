package net.phonex.intellij.android.dbmodel.typeserializers;

import com.intellij.psi.PsiType;
import net.phonex.intellij.android.dbmodel.typeserializers.serializers.BundleSerializer;

/**
 * Custom serializer factory for Date objects
 *
 * @author Dallas Gutauckis [dallas@gutauckis.com]
 */
public class BundleSerializerFactory implements TypeSerializerFactory {
    private final BundleSerializer mSerializer;

    public BundleSerializerFactory() {
        mSerializer = new BundleSerializer();
    }

    @Override
    public TypeSerializer getSerializer(PsiType psiType) {
        if ("android.os.Bundle".equals(psiType.getCanonicalText())) {
            return mSerializer;
        }

        return null;
    }
}
