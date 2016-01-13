package eu.europa.esig.dss.EN319102.validation.vpfswatsp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import eu.europa.esig.dss.jaxb.diagnostic.XmlDigestAlgAndValueType;
import eu.europa.esig.dss.jaxb.diagnostic.XmlSignedObjectsType;
import eu.europa.esig.dss.jaxb.diagnostic.XmlSignedSignature;
import eu.europa.esig.dss.validation.CertificateWrapper;
import eu.europa.esig.dss.validation.TimestampWrapper;
import eu.europa.esig.dss.validation.policy.rules.AttributeValue;
import eu.europa.esig.dss.validation.report.DiagnosticData;

/**
 * 5.6.2.3 POE extraction
 * 5.6.2.3.1 Description
 * This building block derives POEs from a given time-stamp. Assumptions:
 * - The time-stamp validation has returned PASSED.
 * - The cryptographic hash function used in the time-stamp (messageImprint.hashAlgorithm) is considered
 * reliable at current time or, if this is not the case, a PoE for that time-stamp exists for a time when the hash
 * function has still been considered reliable.
 * In the simple case, a time-stamp gives a POE for each data item protected by the time-stamp at the generation
 * date/time of the token.
 * EXAMPLE: A time-stamp on the signature value gives a POE of the signature value at the generation date/time
 * of the time-stamp.
 * A time-stamp can also give an indirect POE when it is computed on the hash value of some data instead of the data
 * itself. A POE for DATA at T1 can be derived from the time-stamp:
 * - If there is a POE for h(DATA) at a date T1,where h is a cryptographic hash function and DATA is some data
 * (e.g. a certificate),
 * - if h is asserted in the cryptographic constraints to be trusted until at least a date T after T1; and
 * - if there is a POE for DATA at a date T after T1.
 */
public class POEExtraction {

	private Map<String, List<Date>> poe = new HashMap<String, List<Date>>();

	public void extractPOE(TimestampWrapper timestamp, DiagnosticData diagnosticData) {

		Date productionTime = timestamp.getProductionTime();

		XmlSignedObjectsType signedObjects = timestamp.getSignedObjects();
		if (signedObjects != null) {
			if (CollectionUtils.isNotEmpty(signedObjects.getSignedSignature())) {
				// SIGNATURES and TIMESTAMPS
				for (XmlSignedSignature signedSignature : signedObjects.getSignedSignature()) {
					addPOE(signedSignature.getId(), productionTime);
				}
			}

			if (CollectionUtils.isNotEmpty(signedObjects.getDigestAlgAndValue())) {
				for (XmlDigestAlgAndValueType digestAlgoAndValue : signedObjects.getDigestAlgAndValue()) {
					if (AttributeValue.CERTIFICATE.equals(digestAlgoAndValue.getCategory())) {
						String certificateId = getCertificateIdByDigest(digestAlgoAndValue, diagnosticData);
						if (certificateId != null) {
							addPOE(certificateId, productionTime);
						}
					}
					// TODO REVOCATION
					// else if (AttributeValue.REVOCATION.equals(digestAlgoAndValue.getCategory())) {
					//
					// }
				}
			}
		}
	}

	private String getCertificateIdByDigest(XmlDigestAlgAndValueType digestAlgoValue, DiagnosticData diagnosticData) {
		List<CertificateWrapper> certificates = diagnosticData.getUsedCertificates();
		if (CollectionUtils.isNotEmpty(certificates)) {
			for (CertificateWrapper certificate : certificates) {
				List<XmlDigestAlgAndValueType> digestAlgAndValues = certificate.getDigestAlgAndValue();
				if (CollectionUtils.isNotEmpty(digestAlgAndValues)) {
					for (XmlDigestAlgAndValueType certificateDigestAndValue : digestAlgAndValues) {
						if (StringUtils.equals(certificateDigestAndValue.getDigestMethod(), digestAlgoValue.getDigestMethod())
								&& StringUtils.equals(certificateDigestAndValue.getDigestValue(), digestAlgoValue.getDigestValue())) {
							return certificate.getId();
						}
					}
				}
			}
		}
		return null;
	}

	private void addPOE(String poeId, Date productionTime) {
		List<Date> datesById = poe.get(poeId);
		if (datesById == null) {
			datesById = new ArrayList<Date>();
			poe.put(poeId, datesById);
		}
		datesById.add(productionTime);
	}

	/**
	 * Returns true if there is a POE exists for a given id at or before the control time.
	 *
	 */
	public boolean isPOEExists(final String id, final Date controlTime) {
		List<Date> dates = poe.get(id);
		if (dates != null) {
			for (Date date : dates) {
				if (date.compareTo(controlTime) <= 0) {
					return true;
				}
			}
		}
		return false;
	}

}
