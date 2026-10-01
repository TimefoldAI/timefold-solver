package ai.timefold.solver.service.json.impl.schema;

import ai.timefold.solver.service.json.impl.schema.ext.UniqueItemsValidatorWithProperty;

import com.networknt.schema.AdditionalPropertiesValidator;
import com.networknt.schema.AllOfValidator;
import com.networknt.schema.AnyOfValidator;
import com.networknt.schema.ConstValidator;
import com.networknt.schema.ContainsValidator;
import com.networknt.schema.DependenciesValidator;
import com.networknt.schema.DependentRequired;
import com.networknt.schema.DependentSchemas;
import com.networknt.schema.EnumValidator;
import com.networknt.schema.ExclusiveMaximumValidator;
import com.networknt.schema.ExclusiveMinimumValidator;
import com.networknt.schema.FalseValidator;
import com.networknt.schema.FormatValidator;
import com.networknt.schema.IfValidator;
import com.networknt.schema.ItemsValidator;
import com.networknt.schema.ItemsValidator202012;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.MaxItemsValidator;
import com.networknt.schema.MaxLengthValidator;
import com.networknt.schema.MaxPropertiesValidator;
import com.networknt.schema.MaximumValidator;
import com.networknt.schema.MinItemsValidator;
import com.networknt.schema.MinLengthValidator;
import com.networknt.schema.MinMaxContainsValidator;
import com.networknt.schema.MinPropertiesValidator;
import com.networknt.schema.MinimumValidator;
import com.networknt.schema.MultipleOfValidator;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.NotAllowedValidator;
import com.networknt.schema.NotValidator;
import com.networknt.schema.OneOfValidator;
import com.networknt.schema.PatternPropertiesValidator;
import com.networknt.schema.PatternValidator;
import com.networknt.schema.PrefixItemsValidator;
import com.networknt.schema.PropertiesValidator;
import com.networknt.schema.PropertyNamesValidator;
import com.networknt.schema.ReadOnlyValidator;
import com.networknt.schema.RecursiveRefValidator;
import com.networknt.schema.RefValidator;
import com.networknt.schema.RequiredValidator;
import com.networknt.schema.TrueValidator;
import com.networknt.schema.TypeValidator;
import com.networknt.schema.UnevaluatedItemsValidator;
import com.networknt.schema.UnevaluatedPropertiesValidator;
import com.networknt.schema.UnionTypeValidator;
import com.networknt.schema.UniqueItemsValidator;
import com.networknt.schema.WriteOnlyValidator;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Empty class that defines all types needed to be registered for reflection for JsonSchema
 */
@RegisterForReflection(targets = {
        JsonSchema.class,
        TypeValidator.class,
        PropertiesValidator.class,
        AdditionalPropertiesValidator.class,
        AllOfValidator.class,
        AnyOfValidator.class,
        ConstValidator.class,
        ContainsValidator.class,
        DependenciesValidator.class,
        DependentRequired.class,
        DependentSchemas.class,
        EnumValidator.class,
        ExclusiveMaximumValidator.class,
        ExclusiveMinimumValidator.class,
        FalseValidator.class,
        FormatValidator.class,
        IfValidator.class,
        ItemsValidator.class,
        ItemsValidator202012.class,
        MaximumValidator.class,
        MaxItemsValidator.class,
        MaxLengthValidator.class,
        MaxPropertiesValidator.class,
        MinimumValidator.class,
        MinItemsValidator.class,
        MinLengthValidator.class,
        MinMaxContainsValidator.class,
        MinPropertiesValidator.class,
        MultipleOfValidator.class,
        NotAllowedValidator.class,
        NotValidator.class,
        OneOfValidator.class,
        PatternPropertiesValidator.class,
        PatternValidator.class,
        PrefixItemsValidator.class,
        PropertyNamesValidator.class,
        ReadOnlyValidator.class,
        RecursiveRefValidator.class,
        RefValidator.class,
        RequiredValidator.class,
        TrueValidator.class,
        UnevaluatedItemsValidator.class,
        UnevaluatedPropertiesValidator.class,
        UnionTypeValidator.class,
        UniqueItemsValidator.class,
        WriteOnlyValidator.class,
        NonValidationKeyword.class,
        UniqueItemsValidatorWithProperty.class

})
public class JsonSchemaValidatorConfig {

}
