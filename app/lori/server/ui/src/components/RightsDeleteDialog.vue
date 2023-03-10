<script lang="ts">
import api from "@/api/api";
import { defineComponent, PropType, ref } from "vue";
import { RightRest } from "@/generated-sources/openapi";
import { ChangeType, useHistoryStore } from "@/stores/history";
import error from "@/utils/error";

export default defineComponent({
  props: {
    right: {
      type: {} as PropType<RightRest>,
      required: true,
    },
    index: {
      type: Number,
      required: true,
    },
    metadataId: {
      type: String,
      required: true,
    },
  },
  // Emits
  emits: ["deleteDialogClosed", "deleteSuccessful"],

  setup(props, { emit }) {
    const deleteAlertError = ref(false);
    const deleteInProgress = ref(false);
    const deleteErrorMessage = ref("");
    const deleteError = ref(false);
    const historyStore = useHistoryStore();

    const close = () => {
      emit("deleteDialogClosed");
    };

    const deleteRight = () => {
      deleteInProgress.value = true;
      deleteAlertError.value = false;
      deleteError.value = false;

      if (props.right.rightId == undefined) {
        deleteErrorMessage.value = "A right-id is missing!";
        deleteError.value = true;
        deleteAlertError.value = true;
      } else {
        api
          .deleteItemRelation(props.metadataId, props.right.rightId)
          .then(() => {
            historyStore.addEntry({
              type: ChangeType.DELETED,
              rightId: props.right.rightId,
            });
            emit("deleteSuccessful", props.index);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              deleteErrorMessage.value = errMsg;
              deleteError.value = true;
              deleteAlertError.value = true;
            });
          })
          .finally(() => {
            deleteInProgress.value = false;
          });
      }
    };

    return {
      // variables
      deleteAlertError,
      deleteErrorMessage,
      deleteInProgress,
      historyStore,
      // methods
      deleteRight,
      close,
    };
  },
});
</script>
<style scoped></style>

<template>
  <v-card>
    <v-card-title class="text-h5">L??schen best??tigen</v-card-title>
    <v-alert v-model="deleteAlertError" dismissible text type="error">
      L??schen war nicht erfolgreich:
      {{ deleteErrorMessage }}
    </v-alert>
    <v-card-text>
      M??chtest du diese Rechteinformation wirklich l??schen?
    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn :disabled="deleteInProgress" color="blue darken-1" @click="close"
        >Abbrechen
      </v-btn>
      <v-btn :loading="deleteInProgress" color="error" @click="deleteRight">
        L??schen
      </v-btn>
      <v-spacer></v-spacer>
    </v-card-actions>
  </v-card>
</template>
