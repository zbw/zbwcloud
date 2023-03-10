import { defineStore } from "pinia";
import { Ref, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const accessStateIdx: Ref<Array<boolean>> = ref([]);
  const accessStateReceived: Ref<Array<string>> = ref([]);
  const accessStateSelectedLastSearch: Ref<Array<string>> = ref([]);
  const accessStateClosed = ref(false);
  const accessStateOpen = ref(false);
  const accessStateRestricted = ref(false);

  const hasLicenceContract = ref(false);
  const hasOpenContentLicence = ref(false);
  const hasZbwUserAgreement = ref(false);

  const formalRuleLicenceContract = ref(false);
  const formalRuleOpenContentLicence = ref(false);
  const formalRuleUserAgreement = ref(false);

  const publicationDateFrom = ref("");
  const publicationDateTo = ref("");

  const paketSigelIdIdx: Ref<Array<boolean>> = ref([]);
  const paketSigelIdReceived: Ref<Array<string>> = ref([]);
  const paketSigelSelectedLastSearch: Ref<Array<string>> = ref([]);

  const publicationTypeIdx: Ref<Array<boolean>> = ref([]);
  const publicationTypeReceived: Ref<Array<string>> = ref([]);
  const publicationTypeSelectedLastSearch: Ref<Array<string>> = ref([]);

  const temporalEventInput = ref("");
  const temporalEventStartDateFilter = ref(false);
  const temporalEventEndDateFilter = ref(false);

  const temporalValidityFilterFuture = ref(false);
  const temporalValidityFilterPresent = ref(false);
  const temporalValidityFilterPast = ref(false);
  const temporalValidOn = ref("");

  const zdbIdIdx: Ref<Array<boolean>> = ref([]);
  const zdbIdReceived: Ref<Array<string>> = ref([]);
  const zdbIdSelectedLastSearch: Ref<Array<string>> = ref([]);

  return {
    lastSearchTerm,
    accessStateIdx,
    accessStateSelectedLastSearch,
    accessStateClosed,
    accessStateRestricted,
    accessStateOpen,
    accessStateReceived,
    formalRuleLicenceContract,
    formalRuleOpenContentLicence,
    formalRuleUserAgreement,
    hasLicenceContract,
    hasOpenContentLicence,
    hasZbwUserAgreement,
    paketSigelIdIdx,
    paketSigelIdReceived,
    paketSigelSelectedLastSearch,
    publicationTypeIdx,
    publicationTypeReceived,
    publicationTypeSelectedLastSearch,
    publicationDateFrom,
    publicationDateTo,
    temporalEventInput,
    temporalEventStartDateFilter,
    temporalEventEndDateFilter,
    temporalValidityFilterFuture,
    temporalValidityFilterPast,
    temporalValidityFilterPresent,
    temporalValidOn,
    zdbIdIdx,
    zdbIdReceived,
    zdbIdSelectedLastSearch,
  };
});
