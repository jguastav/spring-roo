package org.springframework.roo.addon.test;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataPersistenceTags;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link IntegrationTestMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class IntegrationTestMetadataProvider extends AbstractItdMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// Integration test classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}
		
		// We need to parse the annotation, which we expect to be present
		IntegrationTestAnnotationValues annotationValues = new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getEntity() == null) {
			return null;
		}
		
		// Lookup the DOD's metadata. The DOD must conform to the DOD naming conventions
		JavaType dodJavaType = new JavaType(annotationValues.getEntity().getFullyQualifiedTypeName() + "DataOnDemand");
		String dataOnDemandMetadataKey = DataOnDemandMetadata.createIdentifier(dodJavaType, Path.SRC_TEST_JAVA);
		DataOnDemandMetadata dataOnDemandMetadata = (DataOnDemandMetadata) metadataService.get(dataOnDemandMetadataKey);
		if (dataOnDemandMetadata == null || !dataOnDemandMetadata.isValid()) {
			return null;
		}
		
		
		// Lookup the entity's metadata
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(annotationValues.getEntity(), Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (classOrInterfaceTypeDetails == null) {
			return null;
		}
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(IntegrationTestOperationsImpl.class.getName(), classOrInterfaceTypeDetails);
		
		MethodMetadata identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.IDENTIFIER_ACCESSOR_METHOD);
		MethodMetadata versionAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.VERSION_ACCESSOR_METHOD);
		MethodMetadata countMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.COUNT_ALL_METHOD);
		MethodMetadata findMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.FIND_METHOD);
		MethodMetadata findAllMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.FIND_ALL_METHOD);
		MethodMetadata findEntriesMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.FIND_ENTRIES_METHOD);
		MethodMetadata flushMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.FLUSH_METHOD);
		MethodMetadata mergeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.MERGE_METHOD);
		MethodMetadata persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.PERSIST_METHOD);
		MethodMetadata removeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, CustomDataPersistenceTags.REMOVE_METHOD);
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(dataOnDemandMetadataKey, metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
				
		return new IntegrationTestMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, projectMetadata, annotationValues, dataOnDemandMetadata, identifierAccessorMethod, versionAccessorMethod, countMethod, findMethod, findAllMethod, findEntriesMethod, flushMethod, mergeMethod, persistMethod, removeMethod);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "IntegrationTest";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
		Path path = IntegrationTestMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return IntegrationTestMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IntegrationTestMetadata.getMetadataIdentiferType();
	}
}
