package net.phonex.intellij.android.dbmodel.typeserializers;

import com.intellij.psi.PsiType;
import net.phonex.intellij.android.dbmodel.typeserializers.serializers.ParcelableListSerializer;
import net.phonex.intellij.android.dbmodel.util.PsiUtils;

/**
 * Serializer factory for List fields of Parcelable objects
 *
 * @author Dallas Gutauckis [dallas@gutauckis.com]
 */
public class ParcelableListSerializerFactory implements TypeSerializerFactory {

    private ParcelableListSerializer mSerializer;

    public ParcelableListSerializerFactory() {
        mSerializer = new ParcelableListSerializer();
    }

    @Override
    public TypeSerializer getSerializer(PsiType psiType) {
        // There might actually be a way to do this w/ a Collection, but it might not be order-safe
        if (PsiUtils.isTypedClass(psiType, "java.util.List", "android.os.Parcelable")) {
            return mSerializer;
        }

        return null;
    }
}
