<script lang="ts">
import {
  ItemInformation,
  ItemRest,
  MetadataRest,
} from "@/generated-sources/openapi";
import api from "@/api/api";
import { DataTableHeader } from "vuetify";
import GroupOverview from "@/components/GroupOverview.vue";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import SearchFilter from "@/components/SearchFilter.vue";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useDialogsStore } from "@/stores/dialogs";
import error from "@/utils/error";

export default defineComponent({
  components: { GroupOverview, RightsView, MetadataView, SearchFilter },

  setup() {
    const items: Ref<Array<ItemRest>> = ref([]);
    const currentItem = ref({} as ItemRest);
    const headersValueVSelect = ref([]);
    const selectedItems = ref([]);
    const searchTerm = ref("");
    const tableContentLoading = ref(true);

    /**
     * Error handling>
     */
    const hasSearchTokenWithNoKeyError = ref(false);
    const hasSearchTokenWithNoKeyErrorMsg = ref("");
    const invalidSearchKeyError = ref(false);
    const invalidSearchKeyErrorMsg = ref("");
    const loadAlertError = ref(false);
    const loadAlertErrorMessage = ref("");

    const headers = [
      {
        text: "Item-Id",
        align: "start",
        sortable: false,
        value: "metadataId",
      },
      {
        text: "Titel",
        sortable: true,
        value: "title",
        width: "300px",
      },
      {
        text: "Handle",
        sortable: true,
        value: "handle",
      },
      {
        text: "Community",
        sortable: true,
        value: "communityName",
      },
      {
        text: "Collection",
        sortable: true,
        value: "collectionName",
      },
      {
        text: "Publikationstyp",
        sortable: true,
        value: "publicationType",
      },
      {
        text: "Publikationsjahr",
        sortable: true,
        value: "publicationDate",
      },
      {
        text: "Band",
        value: "band",
      },
      {
        text: "DOI",
        value: "doi",
      },
      {
        text: "ISBN",
        value: "isbn",
      },
      {
        text: "ISSN",
        value: "issn",
      },
      {
        text: "Paket-Sigel",
        value: "paketSigel",
      },
      {
        text: "PPN",
        value: "ppn",
      },
      {
        text: "Rechte-K10Plus",
        value: "rightsK10plus",
      },
      {
        text: "Titel Journal",
        value: "titleJournal",
      },
      {
        text: "Titel Serie",
        value: "titleSeries",
      },
      {
        text: "ZDB-ID",
        value: "zdbId",
      },
    ] as Array<DataTableHeader>;

    const selectedHeaders = ref(headers.slice(0, 6));

    const currentPage = ref(1);
    const pageSize = ref(25); // Default page size is 25
    const pageSizes = ref<Array<number>>([5, 10, 25, 50]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);

    // Page changes
    const handlePageChange = (nextPage: number) => {
      currentPage.value = nextPage;
      searchQuery();
    };

    const handlePageSizeChange = (size: number) => {
      pageSize.value = size;
      currentPage.value = 1;
      searchQuery();
    };

    /**
     * Row selection management.
     *
     * Single Click
     */
    const addActiveItem = (metadata: MetadataRest, row: any) => {
      row.select(true);
      let item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === metadata.metadataId
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
    };

    /** Double Click **/
    const setActiveItem = (clickevent: any, row: any) => {
      row.select(true);
      let item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === row.item.metadataId
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
      selectedItems.value = selectedItems.value.filter(
        (e: MetadataRest) => e.metadataId == row.item.metadataId
      );
    };

    const getAlertLoad = () => {
      return loadAlertError;
    };

    onMounted(() => startSearch());

    watch(headersValueVSelect, (currentValue, oldValue) => {
      selectedHeaders.value = currentValue;
    });

    // Search
    const searchStore = useSearchStore();

    const startSearch = () => {
      currentPage.value = 1;
      searchStore.lastSearchTerm = searchTerm.value;
      invalidSearchKeyError.value = false;
      hasSearchTokenWithNoKeyError.value = false;

      searchQuery();
    };

    const searchQuery = () => {
      api
        .searchQuery(
          searchTerm.value,
          (currentPage.value - 1) * pageSize.value,
          pageSize.value,
          pageSize.value,
          buildPublicationDateFilter(),
          buildPublicationTypeFilter(),
          buildAccessStateFilter(),
          buildTempValFilter(),
          buildStartDateAtFilter(),
          buildEndDateAtFilter(),
          buildFormalRuleFilter(),
          buildValidOnFilter(),
          buildPaketSigelIdFilter(),
          buildZDBIdFilter()
        )
        .then((response: ItemInformation) => {
          items.value = response.itemArray;
          tableContentLoading.value = false;
          totalPages.value = response.totalPages;
          numberOfResults.value = response.numberOfResults;
          if (response.invalidSearchKey?.length || 0 > 0) {
            invalidSearchKeyErrorMsg.value =
              "Die folgenden Suchkeys sind ung??ltig: " +
                response.invalidSearchKey?.join(", ") || "";
            invalidSearchKeyError.value = true;
          }

          if (response.hasSearchTokenWithNoKey == true) {
            console.log('hihihe')
            hasSearchTokenWithNoKeyErrorMsg.value =
              "Mindestens ein Wort enth??lt keinen Suchkey." +
              " Dieser Teil wird bei der Suche ignoriert.";
            hasSearchTokenWithNoKeyError.value = true;
          }
          if (response.paketSigels != undefined) {
            searchStore.paketSigelIdReceived = response.paketSigels;
          }
          if (response.hasLicenceContract != undefined) {
            searchStore.hasLicenceContract = response.hasLicenceContract;
          }
          if (response.hasOpenContentLicence != undefined) {
            searchStore.hasOpenContentLicence = response.hasOpenContentLicence;
          }
          if (response.hasZbwUserAgreement != undefined) {
            searchStore.hasZbwUserAgreement = response.hasZbwUserAgreement;
          }
          // Reset AccessState
          searchStore.accessStateReceived =
            response.accessState != undefined ? response.accessState : Array(0);
          searchStore.accessStateIdx = Array(
            searchStore.accessStateReceived.length
          ).fill(false);
          resetDynamicFilter(
            searchStore.accessStateReceived,
            searchStore.accessStateSelectedLastSearch,
            searchStore.accessStateIdx
          );
          // Reset Paket Sigel
          searchStore.paketSigelIdReceived =
            response.paketSigels != undefined ? response.paketSigels : Array(0);
          searchStore.paketSigelIdIdx = Array(
            searchStore.paketSigelIdReceived.length
          ).fill(false);
          resetDynamicFilter(
            searchStore.paketSigelIdReceived,
            searchStore.paketSigelSelectedLastSearch,
            searchStore.paketSigelIdIdx
          );
          // Reset Publication Type
          searchStore.publicationTypeReceived =
            response.publicationType != undefined
              ? response.publicationType.sort()
              : Array(0);
          searchStore.publicationTypeIdx = Array(
            searchStore.publicationTypeReceived.length
          ).fill(false);
          resetDynamicFilter(
            searchStore.publicationTypeReceived,
            searchStore.publicationTypeSelectedLastSearch,
            searchStore.publicationTypeIdx
          );
          // Reset ZDB Id
          searchStore.zdbIdReceived =
            response.zdbIds != undefined ? response.zdbIds : Array(0);
          searchStore.zdbIdIdx = Array(searchStore.zdbIdReceived.length).fill(
            false
          );
          resetDynamicFilter(
            searchStore.zdbIdReceived,
            searchStore.zdbIdSelectedLastSearch,
            searchStore.zdbIdIdx
          );
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            loadAlertErrorMessage.value = errMsg;
            loadAlertError.value = true;
          });
        });
    };

    const resetDynamicFilter = (
      receivedFilters: Array<string>,
      savedFilters: Array<string>,
      idxMap: Array<boolean>
    ) => {
      receivedFilters.forEach((elem: string, index: number): void => {
        if (savedFilters.includes(elem)) {
          idxMap[index] = true;
        }
      });
    };

    const buildPublicationDateFilter: () => string | undefined = () => {
      return searchStore.publicationDateFrom == "" &&
        searchStore.publicationDateTo == ""
        ? undefined
        : searchStore.publicationDateFrom + "-" + searchStore.publicationDateTo;
    };

    const buildPaketSigelIdFilter: () => string | undefined = () => {
      let paketSigelIds: Array<string> = [];
      searchStore.paketSigelIdIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            paketSigelIds.push(searchStore.paketSigelIdReceived[index]);
          }
        }
      );
      // Remind selected ids, for resetting the filter afterwards correctly.
      searchStore.paketSigelSelectedLastSearch = paketSigelIds;
      if (paketSigelIds.length == 0) {
        return undefined;
      } else {
        return paketSigelIds.join(",");
      }
    };

    const buildZDBIdFilter: () => string | undefined = () => {
      let zdbIds: Array<string> = [];
      searchStore.zdbIdIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            zdbIds.push(searchStore.zdbIdReceived[index]);
          }
        }
      );
      // Remind selected ids, for resetting the filter afterwards correctly.
      searchStore.zdbIdSelectedLastSearch = zdbIds;
      if (zdbIds.length == 0) {
        return undefined;
      } else {
        return zdbIds.join(",");
      }
    };

    const buildAccessStateFilter = () => {
      let accessStates: Array<string> = [];
      searchStore.accessStateIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            accessStates.push(
              searchStore.accessStateReceived[index].toUpperCase()
            );
          }
        }
      );

      // Remind selected ids, for resetting the filter afterwards correctly.
      searchStore.accessStateSelectedLastSearch = accessStates.map((value) =>
        value.toLowerCase()
      );
      if (accessStates.length == 0) {
        return undefined;
      } else {
        return accessStates.join(",");
      }
    };

    const buildFormalRuleFilter = () => {
      let formalRule: Array<string> = [];
      if (searchStore.formalRuleLicenceContract) {
        formalRule.push("LICENCE_CONTRACT");
      }
      if (searchStore.formalRuleOpenContentLicence) {
        formalRule.push("OPEN_CONTENT_LICENCE");
      }
      if (searchStore.formalRuleUserAgreement) {
        formalRule.push("ZBW_USER_AGREEMENT");
      }
      if (formalRule.length == 0) {
        return undefined;
      } else {
        return formalRule.join(",");
      }
    };

    const buildTempValFilter = () => {
      let tempVal: Array<string> = [];
      if (searchStore.temporalValidityFilterFuture) {
        tempVal.push("FUTURE");
      }
      if (searchStore.temporalValidityFilterPast) {
        tempVal.push("PAST");
      }
      if (searchStore.temporalValidityFilterPresent) {
        tempVal.push("PRESENT");
      }
      if (tempVal.length == 0) {
        return undefined;
      } else {
        return tempVal.join(",");
      }
    };

    const buildStartDateAtFilter = () => {
      if (
        searchStore.temporalEventStartDateFilter &&
        searchStore.temporalEventInput != ""
      ) {
        return searchStore.temporalEventInput;
      } else {
        return undefined;
      }
    };

    const buildEndDateAtFilter = () => {
      if (
        searchStore.temporalEventEndDateFilter &&
        searchStore.temporalEventInput != ""
      ) {
        return searchStore.temporalEventInput;
      } else {
        return undefined;
      }
    };

    const buildPublicationTypeFilter = () => {
      let types: Array<string> = [];
      let typesFrontend: Array<string> = [];
      searchStore.publicationTypeIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            let modifiedPubTypeFilter: string;
            switch (searchStore.publicationTypeReceived[index]) {
              case "article":
                modifiedPubTypeFilter = "ARTICLE";
                break;
              case "book":
                modifiedPubTypeFilter = "BOOK";
                break;
              case "bookPart":
                modifiedPubTypeFilter = "BOOK_PART";
                break;
              case "conferencePaper":
                modifiedPubTypeFilter = "CONFERENCE_PAPER";
                break;
              case "periodicalPart":
                modifiedPubTypeFilter = "PERIODICAL_PART";
                break;
              case "proceedings":
                modifiedPubTypeFilter = "PROCEEDING";
                break;
              case "researchReport":
                modifiedPubTypeFilter = "RESEARCH_REPORT";
                break;
              case "thesis":
                modifiedPubTypeFilter = "THESIS";
                break;
              case "workingPaper":
                modifiedPubTypeFilter = "WORKING_PAPER";
                break;
              default:
                modifiedPubTypeFilter = "ERROR";
            }
            types.push(modifiedPubTypeFilter);
            typesFrontend.push(searchStore.publicationTypeReceived[index]);
          }
        }
      );
      // Remind selected ids, for resetting the filter afterwards correctly.
      searchStore.publicationTypeSelectedLastSearch = typesFrontend;
      if (types.length == 0) {
        return undefined;
      } else {
        return types.join(",");
      }
    };

    const buildValidOnFilter = () => {
      if (
        searchStore.temporalValidOn != undefined &&
        searchStore.temporalValidOn != ""
      ) {
        return searchStore.temporalValidOn;
      } else {
        return undefined;
      }
    };

    // parse publication type
    const parsePublicationType = (pubType: string) => {
      switch (pubType) {
        case "article":
          return "Article";
        case "book":
          return "Book";
        case "bookPart":
          return "Book Part";
        case "conferencePaper":
          return "Conference Paper";
        case "periodicalPart":
          return "Periodical Part";
        case "proceedings":
          return "Proceedings";
        case "researchReport":
          return "Research Report";
        case "thesis":
          return "Thesis";
        case "workingPaper":
          return "Working Paper";
        default:
          return "Unknown pub type:" + pubType;
      }
    };

    /**
     * Manage Group Dialog.
     */
    const dialogStore = useDialogsStore();

    const closeGroupDialog = () => {
      dialogStore.groupOverviewActivated = false;
    };

    return {
      currentItem,
      currentPage,
      dialogStore,
      hasSearchTokenWithNoKeyError,
      hasSearchTokenWithNoKeyErrorMsg,
      headers,
      headersValueVSelect,
      items,
      invalidSearchKeyError,
      invalidSearchKeyErrorMsg,
      loadAlertError,
      loadAlertErrorMessage,
      numberOfResults,
      pageSize,
      pageSizes,
      searchTerm,
      searchStore,
      selectedHeaders,
      selectedItems,
      tableContentLoading,
      totalPages,
      // Methods
      addActiveItem,
      buildPublicationDateFilter,
      buildPublicationTypeFilter,
      buildAccessStateFilter,
      buildTempValFilter,
      buildStartDateAtFilter,
      buildEndDateAtFilter,
      buildFormalRuleFilter,
      buildValidOnFilter,
      buildPaketSigelIdFilter,
      buildZDBIdFilter,
      closeGroupDialog,
      getAlertLoad,
      handlePageChange,
      handlePageSizeChange,
      parsePublicationType,
      searchQuery,
      setActiveItem,
      startSearch,
    };
  },
});
</script>

<style scoped>
/deep/ tr.v-data-table__selected {
  background: #7d92f5 !important;
}
</style>
<template>
  <v-container>
    <v-dialog
      max-width="1000px"
      v-model="dialogStore.groupOverviewActivated"
      v-on:close="closeGroupDialog"
      :retain-focus="false"
    >
      <GroupOverview></GroupOverview>
    </v-dialog>
    <v-row>
      <v-col cols="2">
        <SearchFilter></SearchFilter>
      </v-col>
      <v-col cols="6">
        <v-card>
          <v-card-title>
            <v-text-field
              v-model="searchTerm"
              append-icon="mdi-magnify"
              label="Suche"
              clearable
              single-line
              @click:append="startSearch"
              @keydown.enter.prevent="startSearch"
              outlined
              persistent-hint
              hint="Sucheingabe: keyword:'suchtext'; Erlaubte Keywords:
              com(Community),
              col(Collection),
              sig(Paket-Sigel),
              tit(Titel),
              zdb(ZDB-Id)"
            ></v-text-field>
          </v-card-title>
          <v-alert
            v-model="invalidSearchKeyError"
            dismissible
            text
            type="error"
          >
            {{ invalidSearchKeyErrorMsg }}
          </v-alert>
          <v-alert
            v-model="hasSearchTokenWithNoKeyError"
            dismissible
            text
            type="error"
          >
            {{ hasSearchTokenWithNoKeyErrorMsg }}
          </v-alert>
          <v-select
            v-model="headersValueVSelect"
            :items="headers"
            label="Spaltenauswahl"
            multiple
            return-object
          >
            <template v-slot:selection="{ item, index }">
              <v-chip v-if="index === 0">
                <span>{{ item.text }}</span>
              </v-chip>
              <span v-if="index === 1" class="grey--text caption"
                >(+{{ headersValueVSelect.length - 1 }} others)</span
              >
            </template>
          </v-select>

          <v-col cols="5" sm="5"> Suchergebnisse: {{ numberOfResults }}</v-col>
          <v-data-table
            disable-pagination
            :hide-default-footer="true"
            :headers="selectedHeaders"
            :items="items.map((value) => value.metadata)"
            @click:row="addActiveItem"
            @dblclick:row="setActiveItem"
            loading="tableContentLoading"
            loading-text="Daten werden geladen... Bitte warten."
            show-select
            item-key="metadataId"
            v-model="selectedItems"
          >
            <template v-slot:item.handle="{ item }">
              <td>
                <a :href="item.handle">{{ item.handle.substring(22, 35) }}</a>
              </td>
            </template>
            <template v-slot:item.publicationType="{ item }">
              <td>{{ parsePublicationType(item.publicationType) }}</td>
            </template>
            <template v-slot:item.publicationDate="{ item }">
              <td>{{ item.publicationDate.toLocaleDateString("de") }}</td>
            </template>
          </v-data-table>
          <v-col cols="14" sm="12">
            <v-row>
              <v-col cols="2" sm="2">
                <v-select
                  v-model="pageSize"
                  :items="pageSizes"
                  label="Eintr??ge pro Seite"
                  @change="handlePageSizeChange"
                ></v-select>
              </v-col>
              <v-col cols="10" sm="9">
                <v-pagination
                  v-model="currentPage"
                  total-visible="7"
                  :length="totalPages"
                  next-icon="mdi-menu-right"
                  prev-icon="mdi-menu-left"
                  @input="handlePageChange"
                ></v-pagination>
              </v-col>
            </v-row>
          </v-col>
          <v-alert v-model="loadAlertError" dismissible text type="error">
            Laden der bibliographischen Daten war nicht erfolgreich:
            {{ loadAlertErrorMessage }}
          </v-alert>
        </v-card>
      </v-col>
      <v-col cols="4">
        <v-card v-if="currentItem.metadata" class="mx-auto" tile>
          <RightsView
            :rights="currentItem.rights"
            :metadataId="currentItem.metadata.metadataId"
            :handle="currentItem.metadata.handle"
          ></RightsView>
          <MetadataView
            :metadata="Object.assign({}, currentItem.metadata)"
          ></MetadataView>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
