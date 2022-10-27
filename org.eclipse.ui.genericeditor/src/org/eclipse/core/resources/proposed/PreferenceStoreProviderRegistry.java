package org.eclipse.core.resources.proposed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.internal.genericeditor.ContentTypeSpecializationComparator;
import org.eclipse.ui.internal.genericeditor.GenericContentTypeRelatedExtension;
import org.eclipse.ui.internal.genericeditor.GenericEditorPlugin;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A registry of {@link IPreferenceStore} provider provided by extension
 * <code>org.eclipse.ui.genericeditor.preferenceStoreProviders</code>. Those
 * extensions are specific to a given {@link IContentType}.
 *
 * @since 1.0
 */
public class PreferenceStoreProviderRegistry {

	private static final String EXTENSION_POINT_ID = GenericEditorPlugin.BUNDLE_ID + ".preferenceStoreProviders"; //$NON-NLS-1$

	private Map<IConfigurationElement, GenericContentTypeRelatedExtension<IPreferenceStoreProvider>> extensions = new HashMap<>();
	private boolean outOfSync = true;

	/**
	 * Creates the registry and binds it to the extension point.
	 */
	public PreferenceStoreProviderRegistry() {
		Platform.getExtensionRegistry().addRegistryChangeListener(event -> outOfSync = true, EXTENSION_POINT_ID);
	}

	/**
	 * Get the contributed {@link IPresentationReconciliers}s that are relevant to
	 * hook on source viewer according to document content types.
	 * 
	 * @param sourceViewer the source viewer we're hooking completion to.
	 * @param editor       the text editor
	 * @param contentTypes the content types of the document we're editing.
	 * @return the list of {@link IPreferenceStore} contributed for at least one of
	 *         the content types.
	 */
	public List<IPreferenceStoreProvider> getPreferenceStoreProviders(ISourceViewer sourceViewer, ITextEditor editor,
			Set<IContentType> contentTypes) {
		if (this.outOfSync) {
			sync();
		}
		return this.extensions.values().stream().filter(ext -> contentTypes.contains(ext.targetContentType))
				.filter(ext -> ext.matches(sourceViewer, editor))
				.sorted(new ContentTypeSpecializationComparator<IPreferenceStoreProvider>())
				.map(GenericContentTypeRelatedExtension<IPreferenceStoreProvider>::createDelegate)
				.collect(Collectors.toList());
	}

	private void sync() {
		Set<IConfigurationElement> toRemoveExtensions = new HashSet<>(this.extensions.keySet());
		for (IConfigurationElement extension : Platform.getExtensionRegistry()
				.getConfigurationElementsFor(EXTENSION_POINT_ID)) {
			toRemoveExtensions.remove(extension);
			if (!this.extensions.containsKey(extension)) {
				try {
					this.extensions.put(extension,
							new GenericContentTypeRelatedExtension<IPreferenceStoreProvider>(extension));
				} catch (Exception ex) {
					GenericEditorPlugin.getDefault().getLog()
							.log(new Status(IStatus.ERROR, GenericEditorPlugin.BUNDLE_ID, ex.getMessage(), ex));
				}
			}
		}
		for (IConfigurationElement toRemove : toRemoveExtensions) {
			this.extensions.remove(toRemove);
		}
		this.outOfSync = false;
	}

}