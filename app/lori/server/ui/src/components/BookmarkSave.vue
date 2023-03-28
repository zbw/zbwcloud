<script lang="ts">
import {
  computed,
  defineComponent,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import bookmarkApi from "@/api/bookmarkApi";
import { useSearchStore } from "@/stores/search";
import searchquerybuilder from "@/utils/searchquerybuilder";
import error from "@/utils/error";
import { required } from "@vuelidate/validators";
import { useVuelidate } from "@vuelidate/core";

export default defineComponent({
  emits: ["addBookmarkSuccessful"],
  setup(props, { emit }) {
    // Vuelidate
    const rules = {
      name: { required },
    };
    const formState = reactive({
      name: "",
    });
    const v$ = useVuelidate(rules, formState);
    const errorName = computed(() => {
      const errors: Array<string> = [];
      if (v$.value.name.$invalid && v$.value.name.$dirty) {
        errors.push("Es wird ein Name benötigt.");
      }
      return errors;
    });

    const updateInProgress = ref(false);
    const description = ref("");
    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");

    const dialogStore = useDialogsStore();
    const searchStore = useSearchStore();
    const close = () => {
      v$.value.$reset();
      updateInProgress.value = false;
      dialogStore.bookmarkSaveActivated = false;
      formState.name = "";
      description.value = "";
    };
    const save = () => {
      v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        updateInProgress.value = true;
        bookmarkApi
          .addRawBookmark(
            formState.name,
            description.value,
            searchStore.lastSearchTerm,
            searchquerybuilder.buildPublicationDateFilter(searchStore),
            searchquerybuilder.buildPublicationTypeFilter(searchStore),
            searchquerybuilder.buildAccessStateFilter(searchStore),
            searchquerybuilder.buildTempValFilter(searchStore),
            searchquerybuilder.buildStartDateAtFilter(searchStore),
            searchquerybuilder.buildEndDateAtFilter(searchStore),
            searchquerybuilder.buildFormalRuleFilter(searchStore),
            searchquerybuilder.buildValidOnFilter(searchStore),
            searchquerybuilder.buildPaketSigelIdFilter(searchStore),
            searchquerybuilder.buildZDBIdFilter(searchStore),
            searchquerybuilder.buildNoRightInformation(searchStore)
          )
          .then((r) => {
            emit("addBookmarkSuccessful", r.bookmarkId);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              updateInProgress.value = false;
              saveAlertError.value = true;
              saveAlertErrorMessage.value = errMsg;
            });
          });
      });
    };
    return {
      description,
      dialogStore,
      errorName,
      formState,
      saveAlertError,
      saveAlertErrorMessage,
      updateInProgress,
      v$,
      close,
      save,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-card>
    <v-container>
      <v-card-title>Suche Speichern</v-card-title>
      <v-row>
        <v-col cols="4">
          <v-subheader>Name</v-subheader>
        </v-col>
        <v-col cols="8">
          <v-text-field
            v-model="formState.name"
            hint="Name des Bookmarks"
            maxlength="256"
            outlined
            :error-messages="errorName"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4">
          <v-subheader>Bookmark-Id</v-subheader>
        </v-col>
        <v-col cols="8">
          <v-text-field
            label="Wird automatisch generiert"
            disabled
            outlined
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4">
          <v-subheader>Beschreibung</v-subheader>
        </v-col>
        <v-col cols="8">
          <v-text-field v-model="description" outlined></v-text-field>
        </v-col>
      </v-row>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text @click="close">Zurück</v-btn>
        <v-btn
          color="blue darken-1"
          text
          @click="save"
          :disabled="updateInProgress"
          >Speichern
        </v-btn>
      </v-card-actions>
      <v-alert v-model="saveAlertError" dismissible text type="error">
        Speichern war nicht erfolgreich:
        {{ saveAlertErrorMessage }}
      </v-alert>
    </v-container>
  </v-card>
</template>